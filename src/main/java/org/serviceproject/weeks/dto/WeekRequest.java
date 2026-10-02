package org.serviceproject.weeks.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request payload for creating or adjusting a week.
 */
public record WeekRequest(
        @NotNull(message = "تاريخ البداية مطلوب")
        LocalDate startDate,

        @NotNull(message = "تاريخ النهاية مطلوب")
        LocalDate endDate
) {}
