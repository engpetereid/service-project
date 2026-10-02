package org.serviceproject.notifications.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.notifications.service.NotificationService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReminderScheduler reminderScheduler;

    @Test
    void executeWeeklyReminderCheck_invokesNotificationService() {
        when(notificationService.sendWeeklyReminders()).thenReturn(3);

        assertDoesNotThrow(() -> reminderScheduler.executeWeeklyReminderCheck());

        verify(notificationService).sendWeeklyReminders();
    }

    @Test
    void executeWeeklyReminderCheck_handlesExceptionGracefully() {
        when(notificationService.sendWeeklyReminders()).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> reminderScheduler.executeWeeklyReminderCheck());

        verify(notificationService).sendWeeklyReminders();
    }
}
