package com.cropdeal.notificationservice.service;

import com.cropdeal.notificationservice.dto.NotificationResponse;
import com.cropdeal.notificationservice.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
