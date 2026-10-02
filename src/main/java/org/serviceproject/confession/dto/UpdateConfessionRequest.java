package org.serviceproject.confession.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request payload for updating a confession record.
 */
public record UpdateConfessionRequest(
        @NotNull(message = "تاريخ الاعتراف مطلوب")
        LocalDate confessionDate,

        String confessionFather,
        String notes
) {}
