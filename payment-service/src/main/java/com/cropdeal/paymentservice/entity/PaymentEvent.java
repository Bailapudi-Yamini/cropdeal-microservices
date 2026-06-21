package com.cropdeal.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Event Sourcing — every state transition of a Payment is recorded here.
 * This table is append-only: rows are never updated or deleted.
 *
 * The full state of any payment can be reconstructed by replaying
 * all events for a given paymentId in occurredAt order.
 *
 * Event types:
 *   PAYMENT_INITIATED  — payment record created
 *   PAYMENT_SUCCESS    — gateway confirmed
 *   PAYMENT_FAILED     — gateway rejected / timeout
 *   PAYMENT_REFUNDED   — refund processed
 */
@Entity
@Table(name = "payment_events", indexes = {
        @Index(name = "idx_pe_payment_id",  columnList = "paymentId"),
        @Index(name = "idx_pe_event_type",  columnList = "eventType"),
        @Index(name = "idx_pe_occurred_at", columnList = "occurredAt")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, length = 50)
    private String eventType;

    /**
     * Full JSON snapshot of the payment state at the time of this event.
     * Stored as TEXT so the event log is self-contained and replayable
     * without joining back to the payments table.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
