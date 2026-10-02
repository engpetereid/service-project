package org.serviceproject.archive.dto;

/**
 * Summary metrics of archive categories scoped to caller authorization.
 */
public record ArchiveSummaryResponse(
        Long deletedPeopleCount,
        long lockedWeeksCount,
        long totalVisitsCount,
        long totalAttendanceSessionsCount,
        long totalConfessionsCount,
        Long auditLogsCount
) {}
