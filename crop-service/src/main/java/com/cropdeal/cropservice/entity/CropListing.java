package com.cropdeal.cropservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "crop_listings", indexes = {
        @Index(name = "idx_farmer_id", columnList = "farmerId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_crop_type", columnList = "cropType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropListing extends BaseEntity {

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private String cropName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CropType cropType;

    @Column(nullable = false)
    private Double quantityAvailable;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private Double pricePerUnit;

    @Column(nullable = false)
    private String location;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CropStatus status = CropStatus.AVAILABLE;

    private LocalDate harvestDate;
}
