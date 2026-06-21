package com.cropdeal.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Published to cropdeal.exchange with routing key: order.placed
 * Consumed by notification-service to alert the farmer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent implements Serializable {
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
