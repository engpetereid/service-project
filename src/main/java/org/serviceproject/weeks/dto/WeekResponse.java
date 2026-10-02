package org.serviceproject.weeks.dto;

import java.time.LocalDate;

/**
 * Response payload for a week with dynamically computed lock status.
 */
public record WeekResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        boolean locked,
        boolean active
) {}
