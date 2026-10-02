package org.serviceproject.notifications.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.notifications.dto.NotificationResponse;
import org.serviceproject.notifications.dto.UnreadCountResponse;
import org.serviceproject.notifications.entity.NotificationType;
import org.serviceproject.notifications.service.NotificationService;
import org.serviceproject.users.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private UserPrincipal principal;
    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(101L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        notificationResponse = new NotificationResponse(
                1L, "تذكير بالافتقاد الأسبوعي", "لديك مخدوم واحد لم يتم افتقاده هذا الأسبوع.",
                NotificationType.WEEKLY_REMINDER, false, null, "WEEK_5", LocalDateTime.now()
        );
    }

    @Test
    void getUserNotifications_returnsOkWithPage() {
        PageResponse<NotificationResponse> page = new PageResponse<>(
                List.of(notificationResponse), 0, 20, 1, 1, true, true
        );
        when(notificationService.getUserNotifications(principal, false, 0, 20)).thenReturn(page);

        ResponseEntity<PageResponse<NotificationResponse>> response =
                notificationController.getUserNotifications(false, 0, 20, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().totalElements());
        assertEquals("تذكير بالافتقاد الأسبوعي", response.getBody().content().get(0).title());
    }

    @Test
    void getUnreadCount_returnsOkWithCount() {
        UnreadCountResponse unread = new UnreadCountResponse(5L);
        when(notificationService.getUnreadCount(principal)).thenReturn(unread);

        ResponseEntity<UnreadCountResponse> response = notificationController.getUnreadCount(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().unreadCount());
    }

    @Test
    void markAsRead_returnsOkWithUpdatedNotification() {
        NotificationResponse readResponse = new NotificationResponse(
                1L, "تذكير بالافتقاد الأسبوعي", "لديك مخدوم واحد لم يتم افتقاده هذا الأسبوع.",
                NotificationType.WEEKLY_REMINDER, true, LocalDateTime.now(), "WEEK_5", LocalDateTime.now()
        );
        when(notificationService.markAsRead(1L, principal)).thenReturn(readResponse);

        ResponseEntity<NotificationResponse> response = notificationController.markAsRead(1L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().read());
    }

    @Test
    void markAllAsRead_returnsOkWithCount() {
        when(notificationService.markAllAsRead(principal)).thenReturn(3);

        ResponseEntity<Map<String, Object>> response = notificationController.markAllAsRead(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().get("markedCount"));
    }

    @Test
    void triggerWeeklyReminders_returnsOkWithSentCount() {
        when(notificationService.triggerWeeklyRemindersManually()).thenReturn(4);

        ResponseEntity<Map<String, Object>> response = notificationController.triggerWeeklyReminders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().get("remindersSent"));
    }
}
