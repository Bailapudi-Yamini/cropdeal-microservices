package com.cropdeal.cropservice.dto;

import com.cropdeal.cropservice.entity.CropStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCropRequest {

    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Double quantityAvailable;

    @Positive(message = "Price must be positive")
    private Double pricePerUnit;

    @Size(max = 1000)
    private String description;

    private String location;

    private CropStatus status;

    private LocalDate harvestDate;
}
