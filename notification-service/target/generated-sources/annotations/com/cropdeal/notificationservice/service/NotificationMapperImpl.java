package com.cropdeal.notificationservice.service;

import com.cropdeal.notificationservice.dto.NotificationResponse;
import com.cropdeal.notificationservice.entity.Notification;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-21T11:46:34+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse.NotificationResponseBuilder notificationResponse = NotificationResponse.builder();

        notificationResponse.createdAt( notification.getCreatedAt() );
        notificationResponse.id( notification.getId() );
        notificationResponse.message( notification.getMessage() );
        notificationResponse.read( notification.isRead() );
        notificationResponse.referenceId( notification.getReferenceId() );
        notificationResponse.title( notification.getTitle() );
        notificationResponse.type( notification.getType() );
        notificationResponse.userId( notification.getUserId() );

        return notificationResponse.build();
    }
}
