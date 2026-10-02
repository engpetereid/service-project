package org.serviceproject.notifications.mapper;

import org.mapstruct.Mapper;
import org.serviceproject.notifications.dto.NotificationResponse;
import org.serviceproject.notifications.entity.Notification;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
