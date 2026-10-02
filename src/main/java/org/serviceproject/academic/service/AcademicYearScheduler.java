package org.serviceproject.academic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.util.DateUtil;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduler that ensures the correct academic year exists and triggers promotion.
 * <p>
 * Runs daily at 00:05 (app timezone). On September 1, creates
 * the new academic year, marks it as current, and executes student promotion.
 * Idempotent — safe to run multiple times.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AcademicYearScheduler {

    private final AcademicYearService academicYearService;
    private final PromotionService promotionService;
    private final AppProperties appProperties;

    /**
     * Daily check at 00:05.
     * Ensures academic year for the current date and triggers idempotent promotion.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "${app.time-zone:Africa/Cairo}")
    public void checkAndCreateAcademicYear() {
        LocalDate today = DateUtil.today(appProperties.timeZone());
        log.debug("Academic year scheduler running for date: {}", today);
        AcademicYear currentYear = academicYearService.ensureAcademicYear(today);

        // Idempotent promotion execution
        try {
            promotionService.executePromotion(currentYear);
        } catch (Exception e) {
            log.error("Automatic promotion execution failed for year: {}", currentYear.getName(), e);
        }
    }
}
