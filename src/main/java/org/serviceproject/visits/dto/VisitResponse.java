package org.serviceproject.visits.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Detailed response for a visit record including snapshots.
 */
public record VisitResponse(
        Long id,
        Long studentId,
        String studentName,
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        boolean weekLocked,
        Long academicYearId,
        String academicYearName,
        Long ministrySnapId,
        String ministrySnapName,
        Long classSnapId,
        String classSnapName,
        Long servantSnapId,
        String servantSnapName,
        String method,
        Integer prayerScore,
        Integer readingScore,
        Integer noteScore,
        String notes,
        Long recordedById,
        String recordedByName,
        LocalDateTime recordedAt
) {}
