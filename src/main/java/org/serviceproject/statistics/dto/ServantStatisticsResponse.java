package org.serviceproject.statistics.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Servant-level detailed statistics response including weekly self-follow-up,
 * student visitation performance, and annual cumulative indicators.
 */
public record ServantStatisticsResponse(
        Long servantId,
        String servantName,
        Long ministryId,
        String ministryName,
        Long classId,
        String className,
        String phone,
        int assignedStudentsCount,
        int visitedCount,
        double visitPercentage,
        Integer servantNoteScore,
        Integer servantMaxNoteScore,
        Double servantNotePercentage,
        Boolean servantAttendedMass,
        Boolean servantAttendedServiceMeeting,
        Boolean servantAttendedTasbeha,
        Boolean servantAttendedManagementMeeting,
        boolean recordedSelfFollowUp,
        Double averagePrayerScore,
        Double averageReadingScore,
        Double averageNoteScore,
        Double annualRecordingRate,
        Double annualAvgNotePercentage,
        Double annualMassRate,
        Double annualMeetingRate,
        Double annualVisitPercentage,
        List<ServantRecentWeekRecord> recentWeeks
) {
    /**
     * Backward-compatible 8-argument constructor.
     */
    public ServantStatisticsResponse(
            Long servantId,
            String servantName,
            int assignedStudentsCount,
            int visitedCount,
            double visitPercentage,
            Double averagePrayerScore,
            Double averageReadingScore,
            Double averageNoteScore
    ) {
        this(
                servantId,
                servantName,
                null,
                null,
                null,
                null,
                null,
                assignedStudentsCount,
                visitedCount,
                visitPercentage,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                averagePrayerScore,
                averageReadingScore,
                averageNoteScore,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList()
        );
    }

    public record ServantRecentWeekRecord(
            Long weekId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            Integer noteScore,
            Integer maxNoteScore,
            Double notePercentage,
            Boolean attendedMass,
            Boolean attendedServiceMeeting,
            int visitedCount,
            int assignedCount,
            double visitPercentage,
            boolean recorded
    ) {}
}
