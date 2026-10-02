package org.serviceproject.attendance.dto;

import java.time.LocalDate;

/**
 * Summary response for an attendance session.
 */
public record AttendanceSessionResponse(
        Long id,
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String activityType,
        LocalDate sessionDate,
        long presentCount
) {}
