package org.serviceproject.notifications.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.notifications.dto.NotificationResponse;
import org.serviceproject.notifications.dto.UnreadCountResponse;
import org.serviceproject.notifications.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for user in-app notifications and weekly reminders.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves paginated notifications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getUserNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        PageResponse<NotificationResponse> response = notificationService.getUserNotifications(principal, unreadOnly, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the count of unread notifications for the notification badge.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {

        UnreadCountResponse response = notificationService.getUnreadCount(principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a specific notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationResponse response = notificationService.markAsRead(id, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks all unread notifications for the user as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        int updatedCount = notificationService.markAllAsRead(principal);
        return ResponseEntity.ok(Map.of("markedCount", updatedCount));
    }

    /**
     * Admin manual trigger for weekly reminders (bypasses the 24-hour window check).
     */
    @PostMapping("/send-reminders")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerWeeklyReminders() {
        int remindersSent = notificationService.triggerWeeklyRemindersManually();
        return ResponseEntity.ok(Map.of("remindersSent", remindersSent));
    }
}
