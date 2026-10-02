package org.serviceproject.reports.dto;

/**
 * Filter parameters for weekly visit follow-up report export.
 */
public record VisitReportFilter(
        Long academicYearId,
        Long weekId,
        Long ministryId,
        Long classId,
        Long servantId
) {}
