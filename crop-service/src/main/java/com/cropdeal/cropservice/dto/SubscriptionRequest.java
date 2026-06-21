package com.cropdeal.cropservice.dto;

import com.cropdeal.cropservice.entity.CropType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {

    @NotNull(message = "Crop type is required")
    private CropType cropType;

    private String preferredLocation;
}
