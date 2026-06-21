package com.cropdeal.cropservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crop_subscriptions", indexes = {
        @Index(name = "idx_dealer_id", columnList = "dealerId"),
        @Index(name = "idx_sub_crop_type", columnList = "cropType")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_dealer_croptype_location",
                columnNames = {"dealerId", "cropType", "preferredLocation"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dealerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CropType cropType;

    private String preferredLocation;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime subscribedAt;
}
