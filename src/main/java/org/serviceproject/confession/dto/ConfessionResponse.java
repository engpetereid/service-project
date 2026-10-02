package org.serviceproject.confession.dto;

import java.time.LocalDate;

/**
 * Response payload for confession records.
 */
public record ConfessionResponse(
        Long id,
        Long studentId,
        String studentName,
        Long academicYearId,
        String academicYearName,
        LocalDate confessionDate,
        String confessionFather,
        String notes,
        Long recordedById,
        String recordedByName
) {}
