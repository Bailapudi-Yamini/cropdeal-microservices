package com.cropdeal.cropservice.dto;

import com.cropdeal.cropservice.entity.CropType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long dealerId;
    private CropType cropType;
    private String preferredLocation;
    private boolean active;
    private LocalDateTime subscribedAt;
}
