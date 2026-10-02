package org.serviceproject.notifications.dto;

import org.serviceproject.notifications.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * DTO representing an in-app notification returned to the client.
 */
public record NotificationResponse(
        Long id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        LocalDateTime readAt,
        String referenceId,
        LocalDateTime createdAt
) {}
