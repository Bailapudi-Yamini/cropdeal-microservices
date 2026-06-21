package com.cropdeal.notificationservice.entity;

public enum NotificationType {
    CROP_POSTED,          // dealer alert: new crop matching subscription
    ORDER_PLACED,         // farmer alert: dealer placed an order
    NEGOTIATION_UPDATE,   // farmer/dealer alert: price proposed, accepted, rejected, countered
    PAYMENT_SUCCESS,      // farmer + dealer alert: payment confirmed, receipt ready
    PAYMENT_FAILED        // dealer alert: payment failed, retry needed
}
