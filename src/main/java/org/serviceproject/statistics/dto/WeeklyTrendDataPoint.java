package org.serviceproject.statistics.dto;

import java.time.LocalDate;

/**
 * Single data point representing weekly summary metrics for trend visualization.
 */
public record WeeklyTrendDataPoint(
        Long weekId,
        String weekLabel,
        LocalDate startDate,
        LocalDate endDate,
        int totalStudents,
        int visitedCount,
        double visitPercentage,
        int massCount,
        double massPercentage,
        int meetingCount,
        double meetingPercentage,
        int tasbehaCount,
        double tasbehaPercentage,
        double overallFollowupIndex
) {}
