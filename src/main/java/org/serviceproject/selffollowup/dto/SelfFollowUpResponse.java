package org.serviceproject.selffollowup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Detailed response for a servant's personal weekly self-follow-up record.
 */
public record SelfFollowUpResponse(
        Long id,
        Long weekId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        boolean weekLocked,
        Long academicYearId,
        String academicYearName,
        Integer noteScore,
        Integer maxNoteScoreSnapshot,
        Boolean attendedMass,
        Boolean attendedServiceMeeting,
        Boolean attendedTasbeha,
        Boolean attendedManagementMeeting,
        Double overallPercentage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
