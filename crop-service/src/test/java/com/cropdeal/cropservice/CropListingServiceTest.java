package com.cropdeal.cropservice;

import com.cropdeal.cropservice.dto.CropListingRequest;
import com.cropdeal.cropservice.dto.CropListingResponse;
import com.cropdeal.cropservice.dto.UpdateCropRequest;
import com.cropdeal.cropservice.entity.*;
import com.cropdeal.cropservice.event.CropPostedEvent;
import com.cropdeal.cropservice.exception.ResourceNotFoundException;
import com.cropdeal.cropservice.exception.UnauthorizedActionException;
import com.cropdeal.cropservice.repository.CropDocumentRepository;
import com.cropdeal.cropservice.repository.CropListingRepository;
import com.cropdeal.cropservice.repository.CropSubscriptionRepository;
import com.cropdeal.cropservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CropListingServiceTest {

    @Mock CropListingRepository listingRepository;
    @Mock CropDocumentRepository documentRepository;
    @Mock CropSubscriptionRepository subscriptionRepository;
    @Mock CropMapper cropMapper;
    @Mock EventPublisher eventPublisher;

    @InjectMocks CropListingServiceImpl cropListingService;

    private CropListing sampleListing;
    private CropListingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleListing = CropListing.builder()
                .farmerId(1L)
                .cropName("Tomato")
                .cropType(CropType.VEGETABLE)
                .quantityAvailable(100.0)
                .unit("kg")
                .pricePerUnit(25.0)
                .location("Pune")
                .status(CropStatus.AVAILABLE)
                .build();

        sampleResponse = CropListingResponse.builder()
                .id(1L)
                .farmerId(1L)
                .cropName("Tomato")
                .cropType(CropType.VEGETABLE)
                .status(CropStatus.AVAILABLE)
                .build();
    }

    @Test
    void createListing_success_publishesEvent() {
        CropListingRequest request = new CropListingRequest();
        request.setCropName("Tomato");
        request.setCropType(CropType.VEGETABLE);
        request.setQuantityAvailable(100.0);
        request.setUnit("kg");
        request.setPricePerUnit(25.0);
        request.setLocation("Pune");

        when(listingRepository.save(any())).thenReturn(sampleListing);
        when(cropMapper.toDocument(any())).thenReturn(new CropDocument());
        when(documentRepository.findByListingId(any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findActiveSubscribers(any(), any())).thenReturn(List.of());
        when(cropMapper.toResponse(sampleListing)).thenReturn(sampleResponse);

        CropListingResponse result = cropListingService.createListing(1L, request);

        assertThat(result.getCropName()).isEqualTo("Tomato");
        verify(eventPublisher).publishCropPosted(any(CropPostedEvent.class));
        verify(documentRepository).save(any(CropDocument.class));
    }

    @Test
    void createListing_withSubscribers_eventContainsDealerIds() {
        CropListingRequest request = new CropListingRequest();
        request.setCropName("Wheat");
        request.setCropType(CropType.GRAIN);
        request.setQuantityAvailable(500.0);
        request.setUnit("quintal");
        request.setPricePerUnit(2000.0);
        request.setLocation("Nashik");

        CropSubscription sub1 = CropSubscription.builder().dealerId(10L).build();
        CropSubscription sub2 = CropSubscription.builder().dealerId(20L).build();

        when(listingRepository.save(any())).thenReturn(sampleListing);
        when(cropMapper.toDocument(any())).thenReturn(new CropDocument());
        when(documentRepository.findByListingId(any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findActiveSubscribers(any(), any()))
                .thenReturn(List.of(sub1, sub2));
        when(cropMapper.toResponse(any())).thenReturn(sampleResponse);

        cropListingService.createListing(1L, request);

        ArgumentCaptor<CropPostedEvent> captor = ArgumentCaptor.forClass(CropPostedEvent.class);
        verify(eventPublisher).publishCropPosted(captor.capture());
        assertThat(captor.getValue().getSubscribedDealerIds()).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void getListingById_notFound_throwsException() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cropListingService.getListingById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateListing_notOwner_throwsUnauthorized() {
        when(listingRepository.findByIdAndFarmerId(1L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cropListingService.updateListing(1L, 99L, new UpdateCropRequest()))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void deleteListing_setsStatusInactive() {
        when(listingRepository.findByIdAndFarmerId(1L, 1L)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any())).thenReturn(sampleListing);
        when(cropMapper.toDocument(any())).thenReturn(new CropDocument());
        when(documentRepository.findByListingId(any())).thenReturn(Optional.empty());

        cropListingService.deleteListing(1L, 1L);

        assertThat(sampleListing.getStatus()).isEqualTo(CropStatus.INACTIVE);
        verify(listingRepository).save(sampleListing);
    }

    @Test
    void markListingAsSold_updatesStatusAndMongo() {
        CropDocument doc = new CropDocument();
        doc.setListingId(1L);
        doc.setStatus("AVAILABLE");

        when(listingRepository.updateStatus(1L, CropStatus.SOLD)).thenReturn(1);
        when(documentRepository.findByListingId(1L)).thenReturn(Optional.of(doc));

        cropListingService.markListingAsSold(1L);

        assertThat(doc.getStatus()).isEqualTo("SOLD");
        verify(documentRepository).save(doc);
    }
}
