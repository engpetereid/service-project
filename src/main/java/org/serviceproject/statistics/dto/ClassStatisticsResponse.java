package org.serviceproject.statistics.dto;

import java.util.List;

/**
 * Class-level detailed statistics response.
 */
public record ClassStatisticsResponse(
        Long classId,
        String className,
        Long ministryId,
        String ministryName,
        DashboardStatisticsResponse dashboard,
        List<ServantStatisticsSummary> servantsStats
) {}
