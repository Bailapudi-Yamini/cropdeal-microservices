package com.cropdeal.orderservice.entity;

public enum OrderStatus {
    PENDING,        // dealer placed order, awaiting farmer acknowledgement
    NEGOTIATING,    // active negotiation round in progress
    CONFIRMED,      // both parties agreed on price — ready for payment
    CANCELLED,      // cancelled by either party or max negotiation rounds exceeded
    COMPLETED       // payment confirmed by payment-service
}
