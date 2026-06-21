package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.dto.CropListingResponse;
import com.cropdeal.cropservice.dto.SubscriptionResponse;
import com.cropdeal.cropservice.entity.CropDocument;
import com.cropdeal.cropservice.entity.CropListing;
import com.cropdeal.cropservice.entity.CropSubscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CropMapper {

    CropListingResponse toResponse(CropListing listing);

    SubscriptionResponse toSubscriptionResponse(CropSubscription subscription);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listingId", source = "id")
    @Mapping(target = "cropType", expression = "java(listing.getCropType().name())")
    @Mapping(target = "status", expression = "java(listing.getStatus().name())")
    @Mapping(target = "farmerName", ignore = true)   // enriched separately if needed
    CropDocument toDocument(CropListing listing);
}
