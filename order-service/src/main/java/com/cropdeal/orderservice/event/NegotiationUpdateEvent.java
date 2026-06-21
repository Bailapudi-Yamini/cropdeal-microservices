package com.cropdeal.orderservice.event;

import com.cropdeal.orderservice.entity.NegotiationStatus;
import com.cropdeal.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Published to cropdeal.exchange with routing key: negotiation.update
 * Consumed by notification-service to alert the other party of the negotiation outcome.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationUpdateEvent implements Serializable {
    private Long negotiationId;
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Long initiatedBy;       // who made this move
    private Long respondingParty;   // who needs to be notified
    private Double proposedPrice;
    private String message;
    private NegotiationStatus negotiationStatus;
    private OrderStatus orderStatus;
    private int roundNumber;
    private LocalDateTime occurredAt;
}
