package com.cropdeal.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Published to cropdeal.exchange with routing key: payment.failed
 * Consumed by notification-service → alerts dealer to retry payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent implements Serializable {
    private Long paymentId;
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;
    private String transactionId;
    private String failureReason;
    private LocalDateTime failedAt;
}
