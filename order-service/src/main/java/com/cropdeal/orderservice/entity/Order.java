package com.cropdeal.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_dealer",  columnList = "dealerId"),
        @Index(name = "idx_order_farmer",  columnList = "farmerId"),
        @Index(name = "idx_order_listing", columnList = "cropListingId"),
        @Index(name = "idx_order_status",  columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(nullable = false)
    private Long cropListingId;

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private Long dealerId;

    @Column(nullable = false)
    private Double quantity;

    /** Starts as the listing's pricePerUnit; updated when a negotiation is accepted */
    @Column(nullable = false)
    private Double agreedPricePerUnit;

    /** quantity * agreedPricePerUnit — recalculated on every price change */
    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(length = 500)
    private String dealerNotes;

    /** Tracks how many negotiation rounds have occurred — enforces max-rounds limit */
    @Builder.Default
    private int negotiationRounds = 0;
}
