package org.serviceproject.statistics.dto;

/**
 * Class-level statistics summary.
 */
public record ClassStatisticsSummary(
        Long classId,
        String className,
        int totalStudents,
        int visitedStudents,
        double visitPercentage,
        int meetingAttendanceCount,
        double meetingAttendancePercentage,
        double overallFollowupIndex
) {}
