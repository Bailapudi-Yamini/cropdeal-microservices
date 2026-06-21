package com.cropdeal.notificationservice.service;

import com.cropdeal.notificationservice.dto.NotificationResponse;
import com.cropdeal.notificationservice.dto.PagedResponse;
import com.cropdeal.notificationservice.entity.Notification;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.exception.ResourceNotFoundException;
import com.cropdeal.notificationservice.exception.UnauthorizedActionException;
import com.cropdeal.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Notification createNotification(Long userId, String title, String message,
                                            NotificationType type, Long referenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("Notification persisted: userId={}, type={}, referenceId={}", userId, type, referenceId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
        return toPagedResponse(page, notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
        return toPagedResponse(page, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        // Verify ownership before marking
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You do not own this notification");
        }

        notificationRepository.markAsRead(notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(userId);
        log.debug("Marked {} notifications as read for userId={}", updated, userId);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page, long unreadCount) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .unreadCount(unreadCount)
                .build();
    }
}
