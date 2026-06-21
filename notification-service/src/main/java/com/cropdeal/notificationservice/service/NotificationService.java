package com.cropdeal.notificationservice.service;

import com.cropdeal.notificationservice.dto.NotificationResponse;
import com.cropdeal.notificationservice.dto.PagedResponse;
import com.cropdeal.notificationservice.entity.Notification;
import com.cropdeal.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /** Persists an in-app notification and optionally sends an email */
    Notification createNotification(Long userId, String title, String message,
                                    NotificationType type, Long referenceId);

    PagedResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable);

    PagedResponse<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
