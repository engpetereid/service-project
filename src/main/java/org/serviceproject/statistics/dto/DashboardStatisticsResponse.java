package org.serviceproject.statistics.dto;

import java.time.LocalDate;

/**
 * Weekly dashboard summary KPIs.
 */
public record DashboardStatisticsResponse(
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        int totalStudents,
        int visitedStudents,
        double visitPercentage,
        int massAttendanceCount,
        double massAttendancePercentage,
        int meetingAttendanceCount,
        double meetingAttendancePercentage,
        int tasbehaAttendanceCount,
        double tasbehaAttendancePercentage,
        double overallFollowupIndex,
        int totalAbsenceAlerts
) {}
