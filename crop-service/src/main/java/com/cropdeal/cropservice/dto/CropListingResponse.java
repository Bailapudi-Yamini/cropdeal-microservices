package com.cropdeal.cropservice.dto;

import com.cropdeal.cropservice.entity.CropStatus;
import com.cropdeal.cropservice.entity.CropType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CropListingResponse {
    private Long id;
    private Long farmerId;
    private String cropName;
    private CropType cropType;
    private Double quantityAvailable;
    private String unit;
    private Double pricePerUnit;
    private String location;
    private String description;
    private CropStatus status;
    private LocalDate harvestDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
