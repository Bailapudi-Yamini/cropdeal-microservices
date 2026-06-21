package com.cropdeal.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user",   columnList = "userId"),
        @Index(name = "idx_notif_type",   columnList = "type"),
        @Index(name = "idx_notif_read",   columnList = "isRead"),
        @Index(name = "idx_notif_ref",    columnList = "referenceId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** ID of the related entity (orderId, paymentId, listingId, etc.) */
    private Long referenceId;

    @Column(name = "isRead", nullable = false)
    @Builder.Default
    private boolean read = false;
}
