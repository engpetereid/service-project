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
        Integer maxNoteScoreSnapshot,
        String notes,
        Long recordedById,
        String recordedByName,
        LocalDateTime recordedAt
) {
    public VisitResponse(Long id, Long studentId, String studentName, Long weekId,
                         LocalDate weekStartDate, LocalDate weekEndDate, boolean weekLocked,
                         Long academicYearId, String academicYearName, Long ministrySnapId,
                         String ministrySnapName, Long classSnapId, String classSnapName,
                         Long servantSnapId, String servantSnapName, String method,
                         Integer prayerScore, Integer readingScore, Integer noteScore,
                         String notes, Long recordedById, String recordedByName,
                         LocalDateTime recordedAt) {
        this(id, studentId, studentName, weekId, weekStartDate, weekEndDate, weekLocked,
             academicYearId, academicYearName, ministrySnapId, ministrySnapName,
             classSnapId, classSnapName, servantSnapId, servantSnapName, method,
             prayerScore, readingScore, noteScore, 21, notes, recordedById, recordedByName, recordedAt);
    }
}
