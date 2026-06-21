package com.cropdeal.notificationservice.repository;

import com.cropdeal.notificationservice.entity.Notification;
import com.cropdeal.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId,
                                                                NotificationType type,
                                                                Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);
}
