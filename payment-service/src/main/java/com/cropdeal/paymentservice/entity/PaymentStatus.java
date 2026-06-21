package com.cropdeal.paymentservice.entity;

public enum PaymentStatus {
    INITIATED,   // payment record created, gateway call pending
    SUCCESS,     // gateway confirmed payment
    FAILED,      // gateway rejected or timed out
    REFUNDED     // payment reversed after success
}
