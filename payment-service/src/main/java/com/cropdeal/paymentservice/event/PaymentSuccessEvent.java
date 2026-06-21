package com.cropdeal.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
    private Long paymentId;
    private Long orderId;
    private Long cropListingId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;
    private String transactionId;
    private String receiptNumber;
}
