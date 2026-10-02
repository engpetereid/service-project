package org.serviceproject.statistics.dto;

/**
 * Servant-level summary for class views.
 */
public record ServantStatisticsSummary(
        Long servantId,
        String servantName,
        int assignedStudentsCount,
        int visitedCount,
        double visitPercentage
) {}
