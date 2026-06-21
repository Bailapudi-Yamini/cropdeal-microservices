package com.cropdeal.adminservice.query.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * QUERY SIDE — read-only projection of payment data.
 * Populated by PaymentSuccessConsumer and PaymentFailedConsumer.
 */
@Entity
@Table(name = "payment_read_model", indexes = {
        @Index(name = "idx_prm_order",   columnList = "orderId"),
        @Index(name = "idx_prm_dealer",  columnList = "dealerId"),
        @Index(name = "idx_prm_farmer",  columnList = "farmerId"),
        @Index(name = "idx_prm_status",  columnList = "paymentStatus"),
        @Index(name = "idx_prm_paid_at", columnList = "paidAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReadModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    private Long orderId;
    private Long farmerId;
    private Long dealerId;
    private Double amount;

    @Column(length = 30)
    private String paymentStatus;   // SUCCESS, FAILED, REFUNDED

    private String transactionId;
    private String receiptNumber;
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime recordedAt;
}
