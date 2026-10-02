package org.serviceproject.statistics.dto;

import java.time.LocalDate;

/**
 * Alert for a student missing visits and meetings for consecutive weeks.
 */
public record AbsenceAlertResponse(
        Long studentId,
        String studentName,
        String phone,
        String guardianPhone,
        Long ministryId,
        String ministryName,
        Long classId,
        String className,
        Long servantId,
        String servantName,
        int consecutiveWeeksAbsent,
        LocalDate lastContactDate
) {}
