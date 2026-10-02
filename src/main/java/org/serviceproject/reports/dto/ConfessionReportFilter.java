package org.serviceproject.reports.dto;

import java.time.LocalDate;

/**
 * Filter parameters for confession records report export.
 */
public record ConfessionReportFilter(
        Long academicYearId,
        Long studentId,
        Long ministryId,
        Long classId,
        LocalDate startDate,
        LocalDate endDate
) {}
