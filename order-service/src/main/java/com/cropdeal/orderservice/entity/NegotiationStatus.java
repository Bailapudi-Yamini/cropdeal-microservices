package com.cropdeal.orderservice.entity;

public enum NegotiationStatus {
    PENDING,    // awaiting response from the other party
    ACCEPTED,   // other party accepted — order moves to CONFIRMED
    REJECTED,   // other party rejected — order moves to CANCELLED
    COUNTERED   // other party countered with a new price
}
