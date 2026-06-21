package com.cropdeal.adminservice.query.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * QUERY SIDE — read-only projection of order data.
 * Populated asynchronously by OrderPlacedConsumer and PaymentSuccessConsumer.
 * Never mutated by any command controller — only by event consumers.
 *
 * This is the CQRS read model: queries run against this table,
 * not against the order-service's write database.
 */
@Entity
@Table(name = "order_read_model", indexes = {
        @Index(name = "idx_orm_dealer",    columnList = "dealerId"),
        @Index(name = "idx_orm_farmer",    columnList = "farmerId"),
        @Index(name = "idx_orm_status",    columnList = "orderStatus"),
        @Index(name = "idx_orm_placed_at", columnList = "placedAt"),
        @Index(name = "idx_orm_crop_type", columnList = "cropType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReadModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    private Long cropListingId;
    private Long farmerId;
    private Long dealerId;
    private Double quantity;
    private Double agreedPricePerUnit;
    private Double totalAmount;

    @Column(length = 30)
    private String orderStatus;   // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Column(length = 30)
    private String cropType;      // denormalized for report filtering

    private String cropName;
    private String location;

    private LocalDateTime placedAt;
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime recordedAt;
}
