package com.cropdeal.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPaymentRequest {

    /**
     * Internal payment DB id — optional.
     * If not provided, orderId is used to look up the payment.
     */
    private Long paymentId;

    /**
     * Order id from order-service — used when paymentId is not known to frontend.
     * At least one of paymentId or orderId must be provided.
     */
    private Long orderId;

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
