package org.serviceproject.confession.dto;

import java.time.LocalDate;

/**
 * Summary of a student's confession status for the overview dashboard.
 */
public record StudentConfessionSummaryDto(
        Long studentId,
        String studentName,
        String phone,
        String gender,
        Long classId,
        String className,
        Long servantId,
        String servantName,
        String confessionFather,
        LocalDate lastConfessionDate,
        Long daysSinceLastConfession,
        int totalConfessionsThisYear,
        String status
) {}
