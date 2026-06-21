package com.cropdeal.notificationservice.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mirror of order-service NegotiationUpdateEvent.
 * Routing key: negotiation.update
 */
@Data
public class NegotiationUpdateEvent {
    private Long negotiationId;
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Long initiatedBy;
    private Long respondingParty;
    private Double proposedPrice;
    private String message;
    private String negotiationStatus;   // String — avoids cross-service enum dependency
    private String orderStatus;
    private int roundNumber;
    private LocalDateTime occurredAt;
}
