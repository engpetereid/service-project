package org.serviceproject.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Date/time utility methods used across the application.
 * <p>
 * All methods that depend on the application timezone accept it as a parameter
 * so the caller can inject it from {@link org.serviceproject.common.config.AppProperties}.
 */
public final class DateUtil {

    private DateUtil() {
        // utility class
    }

    // ── Timezone ─────────────────────────────────────────────────────

    public static ZoneId appZone(String timeZone) {
        return ZoneId.of(timeZone);
    }

    public static LocalDate today(String timeZone) {
        return LocalDate.now(appZone(timeZone));
    }

    public static LocalDateTime now(String timeZone) {
        return LocalDateTime.now(appZone(timeZone));
    }

    // ── Academic Year ────────────────────────────────────────────────

    /**
     * Determines the academic-year name for a given date.
     * <p>
     * Academic year boundary: September 1 → August 31.
     * A date in September or later belongs to {@code year/year+1};
     * a date before September belongs to {@code (year-1)/year}.
     *
     * @return e.g. "2026/2027"
     */
    public static String academicYearName(LocalDate date) {
        int startYear = date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
        return startYear + "/" + (startYear + 1);
    }

    /**
     * Returns the start-year component from a date.
     * E.g. for a date in October 2026 → returns 2026.
     * For a date in March 2027 → returns 2026.
     */
    public static int academicStartYear(LocalDate date) {
        return date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
    }

    public static LocalDate academicYearStartDate(int startYear) {
        return LocalDate.of(startYear, 9, 1);
    }

    public static LocalDate academicYearEndDate(int startYear) {
        return LocalDate.of(startYear + 1, 8, 31);
    }

    // ── Week ─────────────────────────────────────────────────────────

    /**
     * Returns the start date (Friday) of the week containing the given date.
     * Week runs Friday 00:00 → Thursday 23:59:59.
     */
    public static LocalDate weekStartDate(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return date;
        }
        return date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
    }

    /**
     * Returns the end date (Thursday) of the week containing the given date.
     */
    public static LocalDate weekEndDate(LocalDate date) {
        LocalDate start = weekStartDate(date);
        return start.plusDays(6);
    }

    /**
     * Checks if a week is locked based on its end date and the configured lock period.
     *
     * @param weekEndDate the Thursday end date of the week
     * @param lockDays    number of days after week end before lock activates
     * @param timeZone    application timezone
     * @return true if the week is locked
     */
    public static boolean isWeekLocked(LocalDate weekEndDate, int lockDays, String timeZone) {
        LocalDate lockDate = weekEndDate.plusDays(lockDays);
        return today(timeZone).isAfter(lockDate);
    }
}
