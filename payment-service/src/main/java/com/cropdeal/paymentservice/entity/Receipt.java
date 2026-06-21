package com.cropdeal.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipt_payment",  columnList = "paymentId"),
        @Index(name = "idx_receipt_farmer",   columnList = "farmerId"),
        @Index(name = "idx_receipt_dealer",   columnList = "dealerId"),
        @Index(name = "idx_receipt_number",   columnList = "receiptNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private Long dealerId;

    /** Format: RCP-{year}-{paddedSequence}  e.g. RCP-2024-000042 */
    @Column(nullable = false, unique = true, length = 30)
    private String receiptNumber;

    @Column(nullable = false)
    private Double amount;

    /** JSON summary: cropName, quantity, unit, pricePerUnit, orderId */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String cropDetails;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
