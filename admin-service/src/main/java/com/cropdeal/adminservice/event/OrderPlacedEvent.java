package com.cropdeal.adminservice.event;

import lombok.Data;
import java.time.LocalDateTime;

/** Mirror of order-service OrderPlacedEvent — routing key: order.placed */
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
