package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.dto.*;
import com.cropdeal.cropservice.entity.CropStatus;
import com.cropdeal.cropservice.entity.CropType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CropListingService {

    CropListingResponse createListing(Long farmerId, CropListingRequest request);

    CropListingResponse getListingById(Long id);

    PagedResponse<CropListingResponse> getAvailableListings(CropType cropType, String location, Pageable pageable);

    PagedResponse<CropListingResponse> searchListings(String keyword, Pageable pageable);

    CropListingResponse updateListing(Long id, Long farmerId, UpdateCropRequest request);

    void deleteListing(Long id, Long farmerId);

    List<CropListingResponse> getMyListings(Long farmerId);

    // Called by EventConsumer when payment.success arrives
    void markListingAsSold(Long listingId);
}
