package com.cropdeal.adminservice.query.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persisted report record — stores generated report metadata and JSON payload.
 * Allows re-downloading previously generated reports without re-running queries.
 */
@Entity
@Table(name = "admin_reports", indexes = {
        @Index(name = "idx_report_type",       columnList = "reportType"),
        @Index(name = "idx_report_generated",  columnList = "generatedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String reportType;   // ORDER_REPORT, PAYMENT_REPORT, USER_REPORT, DEALER_REPORT

    private String filterBy;     // JSON string of applied filters (dateFrom, dateTo, dealerId, etc.)

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String reportData;   // JSON array of report rows

    private Long generatedBy;    // adminUserId

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
