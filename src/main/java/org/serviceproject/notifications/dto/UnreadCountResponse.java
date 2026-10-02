package org.serviceproject.notifications.dto;

/**
 * DTO for the unread notification badge counter.
 */
public record UnreadCountResponse(
        long unreadCount
) {}
