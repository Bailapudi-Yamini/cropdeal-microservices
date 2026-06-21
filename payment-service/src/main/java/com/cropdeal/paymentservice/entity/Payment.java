package com.cropdeal.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order",   columnList = "orderId"),
        @Index(name = "idx_payment_dealer",  columnList = "dealerId"),
        @Index(name = "idx_payment_farmer",  columnList = "farmerId"),
        @Index(name = "idx_payment_status",  columnList = "status"),
        @Index(name = "idx_payment_txn",     columnList = "transactionId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private Long dealerId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    /** Unique ID generated internally before calling the gateway */
    @Column(unique = true)
    private String transactionId;

    /** Reference returned by the payment gateway on success */
    private String paymentGatewayRef;

    /** Razorpay order ID returned when creating the order — sent to frontend for checkout */
    private String razorpayOrderId;

    /** Raw JSON response from the gateway — stored for audit */
    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;

    private LocalDateTime paidAt;
}
