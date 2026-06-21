package com.cropdeal.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "negotiations", indexes = {
        @Index(name = "idx_neg_order",  columnList = "order_id"),
        @Index(name = "idx_neg_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The order this negotiation round belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** userId of whoever initiated this round (farmer or dealer) */
    @Column(nullable = false)
    private Long initiatedBy;

    @Column(nullable = false)
    private Double proposedPrice;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NegotiationStatus status = NegotiationStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
