package com.cropdeal.paymentservice.entity;

/**
 * String constants for PaymentEvent.eventType.
 * Using a class instead of enum so the values can be used
 * directly in @Column constraints and JSON without .name() calls.
 */
public final class PaymentEventType {
    public static final String PAYMENT_INITIATED = "PAYMENT_INITIATED";
    public static final String PAYMENT_SUCCESS   = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED    = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED  = "PAYMENT_REFUNDED";

    private PaymentEventType() {}
}
