package com.cropdeal.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Published after Razorpay order is created successfully.
 * Frontend polls GET /payments/order/{orderId} to get razorpayOrderId,
 * then opens Razorpay checkout, then calls POST /payments/verify.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCheckoutReadyEvent implements Serializable {
    private Long orderId;
    private Long paymentId;
    private String razorpayOrderId;
    private Double amount;
    private Long dealerId;
    private Long farmerId;
}
