package org.serviceproject.statistics.dto;

import java.util.List;

/**
 * Ministry-level detailed statistics response.
 */
public record MinistryStatisticsResponse(
        Long ministryId,
        String ministryName,
        DashboardStatisticsResponse dashboard,
        List<ClassStatisticsSummary> classesStats
) {}
