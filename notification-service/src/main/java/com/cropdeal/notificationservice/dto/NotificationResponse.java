package com.cropdeal.notificationservice.dto;

import com.cropdeal.notificationservice.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private Long referenceId;
    private boolean read;
    private LocalDateTime createdAt;
}
