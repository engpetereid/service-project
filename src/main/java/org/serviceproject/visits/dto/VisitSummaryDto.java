package org.serviceproject.visits.dto;

import java.time.LocalDateTime;

/**
 * Compact visit summary used inside servant weekly workflow.
 */
public record VisitSummaryDto(
        Long id,
        String method,
        Integer prayerScore,
        Integer readingScore,
        Integer noteScore,
        String notes,
        LocalDateTime recordedAt
) {}
