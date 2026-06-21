package com.cropdeal.cropservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MongoDB read-model for crop listings.
 * Synced from MySQL CropListing on every create/update.
 * Enables fast full-text and geo-based searches without hitting MySQL.
 */
@Document(collection = "crop_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropDocument {

    @Id
    private String id;

    @Indexed
    private Long listingId;       // mirrors CropListing.id from MySQL

    @Indexed
    private Long farmerId;

    private String farmerName;    // denormalized from user-service for display

    @Indexed
    private String cropName;

    @Indexed
    private String cropType;

    private Double quantityAvailable;
    private String unit;
    private Double pricePerUnit;

    @Indexed
    private String location;

    private String description;

    @Indexed
    private String status;

    private LocalDate harvestDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
