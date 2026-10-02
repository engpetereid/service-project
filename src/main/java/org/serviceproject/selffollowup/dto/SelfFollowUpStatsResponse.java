package org.serviceproject.selffollowup.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Statistics and trends for a servant's personal self-follow-up across the academic year.
 */
public record SelfFollowUpStatsResponse(
        long totalWeeks,
        long recordedWeeks,
        double recordingRate,
        Double avgNotePercentage,
        Double avgMassRate,
        Double avgServiceMeetingRate,
        Double avgTasbehaRate,
        Double avgManagementMeetingRate,
        Double avgOverallPercentage,
        List<WeeklyTrendPoint> weeklyTrend
) {
    public record WeeklyTrendPoint(
            Long weekId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            Double overallPercentage
    ) {}
}
