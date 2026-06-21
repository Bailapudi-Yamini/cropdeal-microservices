package com.cropdeal.notificationservice.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirror of crop-service CropPostedEvent.
 * Routing key: crop.posted
 */
@Data
public class CropPostedEvent {
    private Long listingId;
    private Long farmerId;
    private String farmerName;
    private String cropName;
    private String cropType;
    private Double quantityAvailable;
    private String unit;
    private Double pricePerUnit;
    private String location;
    private List<Long> subscribedDealerIds;
    private LocalDateTime postedAt;
}
