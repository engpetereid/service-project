package org.serviceproject.statistics.dto;

import java.util.List;

/**
 * Aggregated servants' performance metrics and list of servant records for a specific scope and week.
 */
public record ServantPerformanceResponse(
        int totalServants,
        int recordedFollowUpCount,
        double followUpSubmissionRate,
        Double averageNotePercentage,
        Double massAttendanceRate,
        Double meetingAttendanceRate,
        Double overallVisitPercentage,
        List<ServantStatisticsSummary> servants
) {}
