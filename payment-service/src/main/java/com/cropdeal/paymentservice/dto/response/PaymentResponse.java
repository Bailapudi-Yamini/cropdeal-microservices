package com.cropdeal.paymentservice.dto.response;

import com.cropdeal.paymentservice.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;
    private PaymentStatus status;
    private String transactionId;
    private String paymentGatewayRef;
    private String razorpayOrderId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
