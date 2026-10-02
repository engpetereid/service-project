package org.serviceproject.statistics.dto;

/**
 * Servant-level detailed statistics response including average scores.
 */
public record ServantStatisticsResponse(
        Long servantId,
        String servantName,
        int assignedStudentsCount,
        int visitedCount,
        double visitPercentage,
        Double averagePrayerScore,
        Double averageReadingScore,
        Double averageNoteScore
) {}
