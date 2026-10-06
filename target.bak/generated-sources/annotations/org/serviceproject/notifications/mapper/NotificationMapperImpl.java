package org.serviceproject.notifications.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.serviceproject.notifications.dto.NotificationResponse;
import org.serviceproject.notifications.entity.Notification;
import org.serviceproject.notifications.entity.NotificationType;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-03T14:11:03+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.20.1 (Ubuntu)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        Long id = null;
        String title = null;
        String message = null;
        NotificationType type = null;
        boolean read = false;
        LocalDateTime readAt = null;
        String referenceId = null;
        LocalDateTime createdAt = null;

        id = notification.getId();
        title = notification.getTitle();
        message = notification.getMessage();
        type = notification.getType();
        read = notification.isRead();
        readAt = notification.getReadAt();
        referenceId = notification.getReferenceId();
        createdAt = notification.getCreatedAt();

        NotificationResponse notificationResponse = new NotificationResponse( id, title, message, type, read, readAt, referenceId, createdAt );

        return notificationResponse;
    }

    @Override
    public List<NotificationResponse> toResponseList(List<Notification> notifications) {
        if ( notifications == null ) {
            return null;
        }

        List<NotificationResponse> list = new ArrayList<NotificationResponse>( notifications.size() );
        for ( Notification notification : notifications ) {
            list.add( toResponse( notification ) );
        }

        return list;
    }
}
