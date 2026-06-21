package com.cropdeal.notificationservice.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mirror of order-service OrderPlacedEvent.
 * Routing key: order.placed
 */
@Data
public class OrderPlacedEvent {
    private Long orderId;
    private Long cropListingId;
    private Long farmerId;
    private Long dealerId;
    private Double quantity;
    private Double agreedPricePerUnit;
    private Double totalAmount;
    private String dealerNotes;
    private LocalDateTime placedAt;
}
