package com.cropdeal.cropservice.dto;

import com.cropdeal.cropservice.entity.CropType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CropListingRequest {

    @NotBlank(message = "Crop name is required")
    @Size(max = 100)
    private String cropName;

    @NotNull(message = "Crop type is required")
    private CropType cropType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantityAvailable;

    @NotBlank(message = "Unit is required")
    @Size(max = 20)
    private String unit;

    @NotNull(message = "Price per unit is required")
    @Positive(message = "Price must be positive")
    private Double pricePerUnit;

    @NotBlank(message = "Location is required")
    private String location;

    @Size(max = 1000)
    private String description;

    @Future(message = "Harvest date must be in the future")
    private LocalDate harvestDate;
}
