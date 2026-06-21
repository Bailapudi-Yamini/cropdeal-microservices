package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.dto.CropListingResponse;
import com.cropdeal.cropservice.dto.SubscriptionResponse;
import com.cropdeal.cropservice.entity.CropDocument;
import com.cropdeal.cropservice.entity.CropListing;
import com.cropdeal.cropservice.entity.CropSubscription;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-21T11:46:15+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CropMapperImpl implements CropMapper {

    @Override
    public CropListingResponse toResponse(CropListing listing) {
        if ( listing == null ) {
            return null;
        }

        CropListingResponse.CropListingResponseBuilder cropListingResponse = CropListingResponse.builder();

        cropListingResponse.createdAt( listing.getCreatedAt() );
        cropListingResponse.cropName( listing.getCropName() );
        cropListingResponse.cropType( listing.getCropType() );
        cropListingResponse.description( listing.getDescription() );
        cropListingResponse.farmerId( listing.getFarmerId() );
        cropListingResponse.harvestDate( listing.getHarvestDate() );
        cropListingResponse.id( listing.getId() );
        cropListingResponse.location( listing.getLocation() );
        cropListingResponse.pricePerUnit( listing.getPricePerUnit() );
        cropListingResponse.quantityAvailable( listing.getQuantityAvailable() );
        cropListingResponse.status( listing.getStatus() );
        cropListingResponse.unit( listing.getUnit() );
        cropListingResponse.updatedAt( listing.getUpdatedAt() );

        return cropListingResponse.build();
    }

    @Override
    public SubscriptionResponse toSubscriptionResponse(CropSubscription subscription) {
        if ( subscription == null ) {
            return null;
        }

        SubscriptionResponse.SubscriptionResponseBuilder subscriptionResponse = SubscriptionResponse.builder();

        subscriptionResponse.active( subscription.isActive() );
        subscriptionResponse.cropType( subscription.getCropType() );
        subscriptionResponse.dealerId( subscription.getDealerId() );
        subscriptionResponse.id( subscription.getId() );
        subscriptionResponse.preferredLocation( subscription.getPreferredLocation() );
        subscriptionResponse.subscribedAt( subscription.getSubscribedAt() );

        return subscriptionResponse.build();
    }

    @Override
    public CropDocument toDocument(CropListing listing) {
        if ( listing == null ) {
            return null;
        }

        CropDocument.CropDocumentBuilder cropDocument = CropDocument.builder();

        cropDocument.listingId( listing.getId() );
        cropDocument.createdAt( listing.getCreatedAt() );
        cropDocument.cropName( listing.getCropName() );
        cropDocument.description( listing.getDescription() );
        cropDocument.farmerId( listing.getFarmerId() );
        cropDocument.harvestDate( listing.getHarvestDate() );
        cropDocument.location( listing.getLocation() );
        cropDocument.pricePerUnit( listing.getPricePerUnit() );
        cropDocument.quantityAvailable( listing.getQuantityAvailable() );
        cropDocument.unit( listing.getUnit() );
        cropDocument.updatedAt( listing.getUpdatedAt() );

        cropDocument.cropType( listing.getCropType().name() );
        cropDocument.status( listing.getStatus().name() );

        return cropDocument.build();
    }
}
