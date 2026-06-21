package com.cropdeal.cropservice.event;

import com.cropdeal.cropservice.entity.CropType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Published to cropdeal.exchange with routing key: crop.posted
 * Consumed by notification-service to alert subscribed dealers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropPostedEvent implements Serializable {

    private Long listingId;
    private Long farmerId;
    private String farmerName;
    private String cropName;
    private CropType cropType;
    private Double quantityAvailable;
    private String unit;
    private Double pricePerUnit;
    private String location;

    /** Dealer IDs resolved at publish time — notification-service fans out to each */
    private List<Long> subscribedDealerIds;

    private LocalDateTime postedAt;
}
