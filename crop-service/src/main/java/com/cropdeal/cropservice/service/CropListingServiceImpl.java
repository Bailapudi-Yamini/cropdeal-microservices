package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.dto.*;
import com.cropdeal.cropservice.entity.*;
import com.cropdeal.cropservice.event.CropPostedEvent;
import com.cropdeal.cropservice.exception.ResourceNotFoundException;
import com.cropdeal.cropservice.exception.UnauthorizedActionException;
import com.cropdeal.cropservice.repository.CropDocumentRepository;
import com.cropdeal.cropservice.repository.CropListingRepository;
import com.cropdeal.cropservice.repository.CropSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CropListingServiceImpl implements CropListingService {

    private final CropListingRepository listingRepository;
    private final CropDocumentRepository documentRepository;
    private final CropSubscriptionRepository subscriptionRepository;
    private final CropMapper cropMapper;
    private final EventPublisher eventPublisher;

    @Override
    public CropListingResponse createListing(Long farmerId, CropListingRequest request) {
        CropListing listing = CropListing.builder()
                .farmerId(farmerId)
                .cropName(request.getCropName())
                .cropType(request.getCropType())
                .quantityAvailable(request.getQuantityAvailable())
                .unit(request.getUnit())
                .pricePerUnit(request.getPricePerUnit())
                .location(request.getLocation())
                .description(request.getDescription())
                .harvestDate(request.getHarvestDate())
                .status(CropStatus.AVAILABLE)
                .build();

        listing = listingRepository.save(listing);

        // Sync to MongoDB read-model (non-critical — failure should not roll back MySQL save)
        try {
            syncToMongo(listing);
        } catch (Exception e) {
            log.warn("Failed to sync listing {} to MongoDB: {}", listing.getId(), e.getMessage());
        }

        final CropListing saved = listing;
        // Publish event outside transaction — messaging failure should not roll back the save
        try {
            publishCropPostedEvent(saved);
        } catch (Exception e) {
            log.warn("Failed to publish crop.posted event for listingId={}: {}", saved.getId(), e.getMessage());
        }

        return cropMapper.toResponse(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public CropListingResponse getListingById(Long id) {
        return cropMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CropListingResponse> getAvailableListings(CropType cropType,
                                                                    String location,
                                                                    Pageable pageable) {
        Page<CropListing> page = listingRepository.searchAvailable(cropType, location, pageable);
        return toPagedResponse(page.map(cropMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CropListingResponse> searchListings(String keyword, Pageable pageable) {
        // Delegate to MongoDB for full-text search
        Page<CropDocument> docs = documentRepository.fullTextSearch(keyword, pageable);
        List<CropListingResponse> content = docs.getContent().stream()
                .map(doc -> cropMapper.toResponse(findById(doc.getListingId())))
                .toList();
        return PagedResponse.<CropListingResponse>builder()
                .content(content)
                .page(docs.getNumber())
                .size(docs.getSize())
                .totalElements(docs.getTotalElements())
                .totalPages(docs.getTotalPages())
                .last(docs.isLast())
                .build();
    }

    @Override
    public CropListingResponse updateListing(Long id, Long farmerId, UpdateCropRequest request) {
        CropListing listing = listingRepository.findByIdAndFarmerId(id, farmerId)
                .orElseThrow(() -> new UnauthorizedActionException(
                        "Listing not found or you do not own this listing"));

        if (request.getQuantityAvailable() != null) {
            listing.setQuantityAvailable(request.getQuantityAvailable());
            if (request.getQuantityAvailable() == 0) listing.setStatus(CropStatus.SOLD);
            else if (request.getQuantityAvailable() > 0 && listing.getStatus() == CropStatus.SOLD) listing.setStatus(CropStatus.AVAILABLE);
        }
        if (request.getPricePerUnit()       != null) listing.setPricePerUnit(request.getPricePerUnit());
        if (request.getDescription()        != null) listing.setDescription(request.getDescription());
        if (request.getLocation()           != null) listing.setLocation(request.getLocation());
        if (request.getStatus()             != null) listing.setStatus(request.getStatus());
        if (request.getHarvestDate()        != null) listing.setHarvestDate(request.getHarvestDate());

        listing = listingRepository.save(listing);
        syncToMongo(listing);

        return cropMapper.toResponse(listing);
    }

    @Override
    public void deleteListing(Long id, Long farmerId) {
        CropListing listing = listingRepository.findByIdAndFarmerId(id, farmerId)
                .orElseThrow(() -> new UnauthorizedActionException(
                        "Listing not found or you do not own this listing"));

        listing.setStatus(CropStatus.INACTIVE);
        listingRepository.save(listing);
        syncToMongo(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CropListingResponse> getMyListings(Long farmerId) {
        return listingRepository.findByFarmerId(farmerId).stream()
                .map(cropMapper::toResponse)
                .toList();
    }

    @Override
    public void markListingAsSold(Long listingId) {
        int updated = listingRepository.updateStatus(listingId, CropStatus.SOLD);
        if (updated == 0) {
            log.warn("markListingAsSold: listing {} not found", listingId);
            return;
        }
        // Sync status change to MongoDB
        documentRepository.findByListingId(listingId).ifPresent(doc -> {
            doc.setStatus(CropStatus.SOLD.name());
            doc.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(doc);
        });
        log.info("Listing {} marked as SOLD", listingId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CropListing findById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CropListing", id));
    }

    private void syncToMongo(CropListing listing) {
        CropDocument doc = cropMapper.toDocument(listing);
        doc.setCreatedAt(listing.getCreatedAt());
        doc.setUpdatedAt(listing.getUpdatedAt());

        // Preserve existing Mongo _id on updates
        documentRepository.findByListingId(listing.getId())
                .ifPresent(existing -> doc.setId(existing.getId()));

        documentRepository.save(doc);
    }

    private void publishCropPostedEvent(CropListing listing) {
        List<Long> subscribedDealerIds = subscriptionRepository
                .findActiveSubscribers(listing.getCropType(), listing.getLocation())
                .stream()
                .map(CropSubscription::getDealerId)
                .distinct()
                .toList();

        CropPostedEvent event = CropPostedEvent.builder()
                .listingId(listing.getId())
                .farmerId(listing.getFarmerId())
                .farmerName(null)           // enriched by notification-service via user-service call
                .cropName(listing.getCropName())
                .cropType(listing.getCropType())
                .quantityAvailable(listing.getQuantityAvailable())
                .unit(listing.getUnit())
                .pricePerUnit(listing.getPricePerUnit())
                .location(listing.getLocation())
                .subscribedDealerIds(subscribedDealerIds)
                .postedAt(LocalDateTime.now())
                .build();

        eventPublisher.publishCropPosted(event);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
