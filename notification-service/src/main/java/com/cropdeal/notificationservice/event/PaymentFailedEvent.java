package com.cropdeal.notificationservice.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mirror of payment-service PaymentFailedEvent.
 * Routing key: payment.failed
 */
@Data
public class PaymentFailedEvent {
    private Long paymentId;
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;
    private String transactionId;
    private String failureReason;
    private LocalDateTime failedAt;
}
