package org.serviceproject.notifications.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.notifications.service.NotificationService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that periodically checks if the current week is in its final 24-hour reminder window
 * and dispatches in-app reminders to servants with unvisited students.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ReminderScheduler {

    private final NotificationService notificationService;

    /**
     * Executes hourly to check if reminders need to be sent.
     */
    @Scheduled(cron = "0 0 * * * *", zone = "${app.time-zone:Africa/Cairo}")
    public void executeWeeklyReminderCheck() {
        log.debug("Executing scheduled weekly reminder check");
        try {
            int remindersSent = notificationService.sendWeeklyReminders();
            if (remindersSent > 0) {
                log.info("Weekly reminder job dispatched {} reminders to servants", remindersSent);
            }
        } catch (Exception e) {
            log.error("Error executing weekly reminder check", e);
        }
    }
}
