package org.serviceproject.academic.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for a promotion run.
 */
public record PromotionRunResponse(
        Long id,
        Long academicYearId,
        String academicYearName,
        String status,
        int promotedCount,
        int graduatedCount,
        int skippedCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
