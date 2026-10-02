package org.serviceproject.selffollowup.dto;

import org.serviceproject.weeks.dto.WeekResponse;

/**
 * Payload for the servant's current week self-follow-up view.
 */
public record SelfFollowUpCurrentWeekResponse(
        WeekResponse week,
        SelfFollowUpResponse record,
        int maxNoteScore
) {}
