package org.serviceproject.statistics.dto;

import org.serviceproject.visits.entity.VisitMethod;
import java.time.LocalDate;
import java.util.List;

/**
 * Detailed student-level spiritual statistics response.
 */
public record StudentStatisticsResponse(
        Long studentId,
        String studentName,
        String phone,
        String ministryName,
        String className,
        String servantName,
        int totalWeeksCount,
        int visitedWeeksCount,
        double visitPercentage,
        int massAttendanceCount,
        double massAttendancePercentage,
        int meetingAttendanceCount,
        double meetingAttendancePercentage,
        int tasbehaAttendanceCount,
        double tasbehaAttendancePercentage,
        Double averagePrayerScore,
        Double averageReadingScore,
        Double averageNoteScore,
        int totalConfessionsCount,
        LocalDate lastConfessionDate,
        List<StudentRecentVisitSummary> recentVisits
) {
    public record StudentRecentVisitSummary(
            Long visitId,
            Long weekId,
            LocalDate weekStartDate,
            VisitMethod method,
            Integer prayerScore,
            Integer readingScore,
            Integer noteScore,
            String notes,
            String servantName,
            LocalDate recordedDate
    ) {}
}
