package com.cropdeal.adminservice.event;

import lombok.Data;
import java.time.LocalDateTime;

/** Mirror of payment-service PaymentSuccessEvent — routing key: payment.success */
@Data
public class PaymentSuccessEvent {
    private Long paymentId;
    private Long orderId;
    private Long cropListingId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;
    private String transactionId;
    private String receiptNumber;
    private LocalDateTime paidAt;
}
