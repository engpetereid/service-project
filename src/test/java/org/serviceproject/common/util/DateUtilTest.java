package org.serviceproject.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DateUtil}.
 */
class DateUtilTest {

    // ── Academic Year ────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "2026-09-01, 2026/2027",   // September 1 → new year starts
            "2026-12-25, 2026/2027",   // December → same year
            "2027-01-15, 2026/2027",   // January → still previous year's cycle
            "2027-06-30, 2026/2027",   // June → still previous year's cycle
            "2027-08-31, 2026/2027",   // August 31 → last day of academic year
            "2027-09-01, 2027/2028",   // September 1 → next academic year
    })
    void academicYearName_correctlyDeterminesYear(String dateStr, String expected) {
        LocalDate date = LocalDate.parse(dateStr);
        assertEquals(expected, DateUtil.academicYearName(date));
    }

    @Test
    void academicStartYear_september() {
        assertEquals(2026, DateUtil.academicStartYear(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void academicStartYear_march() {
        assertEquals(2026, DateUtil.academicStartYear(LocalDate.of(2027, 3, 15)));
    }

    @Test
    void academicYearStartDate_returnsCorrectDate() {
        assertEquals(LocalDate.of(2026, 9, 1), DateUtil.academicYearStartDate(2026));
    }

    @Test
    void academicYearEndDate_returnsCorrectDate() {
        assertEquals(LocalDate.of(2027, 8, 31), DateUtil.academicYearEndDate(2026));
    }

    // ── Week ─────────────────────────────────────────────────────────

    @Test
    void weekStartDate_onFriday_returnsSameDay() {
        LocalDate friday = LocalDate.of(2026, 9, 18); // Friday
        assertEquals(DayOfWeek.FRIDAY, friday.getDayOfWeek());
        assertEquals(friday, DateUtil.weekStartDate(friday));
    }

    @Test
    void weekStartDate_onSaturday_returnsPreviousFriday() {
        LocalDate saturday = LocalDate.of(2026, 9, 19);
        assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
        LocalDate expected = LocalDate.of(2026, 9, 18); // previous Friday
        assertEquals(expected, DateUtil.weekStartDate(saturday));
    }

    @Test
    void weekStartDate_onThursday_returnsFridayOfThatWeek() {
        LocalDate thursday = LocalDate.of(2026, 9, 24); // Thursday (end of week)
        assertEquals(DayOfWeek.THURSDAY, thursday.getDayOfWeek());
        LocalDate expected = LocalDate.of(2026, 9, 18); // the Friday that started this week
        assertEquals(expected, DateUtil.weekStartDate(thursday));
    }

    @Test
    void weekStartDate_onWednesday_returnsPreviousFriday() {
        LocalDate wednesday = LocalDate.of(2026, 9, 23);
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.getDayOfWeek());
        LocalDate expected = LocalDate.of(2026, 9, 18);
        assertEquals(expected, DateUtil.weekStartDate(wednesday));
    }

    @Test
    void weekEndDate_returnsSixDaysAfterStart() {
        LocalDate friday = LocalDate.of(2026, 9, 18);
        LocalDate expectedThursday = LocalDate.of(2026, 9, 24);
        assertEquals(expectedThursday, DateUtil.weekEndDate(friday));
    }

    @Test
    void weekEndDate_alwaysThursday() {
        LocalDate anyDay = LocalDate.of(2026, 9, 21); // Monday
        LocalDate end = DateUtil.weekEndDate(anyDay);
        assertEquals(DayOfWeek.THURSDAY, end.getDayOfWeek());
    }

    // ── Timezone ─────────────────────────────────────────────────────

    @Test
    void appZone_returnsValidZone() {
        ZoneId zone = DateUtil.appZone("Africa/Cairo");
        assertNotNull(zone);
        assertEquals("Africa/Cairo", zone.getId());
    }

    @Test
    void appZone_throwsOnInvalidTimezone() {
        assertThrows(Exception.class, () -> DateUtil.appZone("Invalid/Zone"));
    }

    @Test
    void today_returnsNonNull() {
        LocalDate today = DateUtil.today("Africa/Cairo");
        assertNotNull(today);
    }

    // ── Week Locking ─────────────────────────────────────────────────

    @Test
    void isWeekLocked_returnsFalse_withinLockPeriod() {
        // A week that ended yesterday — well within the 30-day window
        LocalDate weekEnd = DateUtil.today("Africa/Cairo").minusDays(1);
        assertFalse(DateUtil.isWeekLocked(weekEnd, 30, "Africa/Cairo"));
    }

    @Test
    void isWeekLocked_returnsTrue_afterLockPeriod() {
        // A week that ended 31 days ago — past the 30-day lock window
        LocalDate weekEnd = DateUtil.today("Africa/Cairo").minusDays(31);
        assertTrue(DateUtil.isWeekLocked(weekEnd, 30, "Africa/Cairo"));
    }

    @Test
    void isWeekLocked_returnsFalse_exactlyOnLockDay() {
        // A week that ended exactly 30 days ago — today == lockDate, not after
        LocalDate weekEnd = DateUtil.today("Africa/Cairo").minusDays(30);
        assertFalse(DateUtil.isWeekLocked(weekEnd, 30, "Africa/Cairo"));
    }
}
