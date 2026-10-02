package org.serviceproject.weeks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.util.DateUtil;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job that ensures weeks are automatically created on Friday at 00:00 (Cairo time).
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class WeekScheduler {

    private final WeekService weekService;
    private final AppProperties appProperties;

    /**
     * Daily check at midnight (00:00 Cairo time).
     * Ensures the current week exists, plus prepares the upcoming week.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "${app.time-zone:Africa/Cairo}")
    public void ensureWeeksCreated() {
        LocalDate today = DateUtil.today(appProperties.timeZone());
        LocalDate currentWeekStart = DateUtil.weekStartDate(today);

        log.debug("Week scheduler running for date: {}, current week start: {}", today, currentWeekStart);
        weekService.ensureWeek(currentWeekStart);

        // Pre-create next week for smooth transition
        weekService.ensureWeek(currentWeekStart.plusWeeks(1));
    }
}
