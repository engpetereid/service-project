package org.serviceproject.confession.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request payload for recording a confession.
 */
public record CreateConfessionRequest(
        @NotNull(message = "المخدوم مطلوب")
        Long studentId,

        @NotNull(message = "تاريخ الاعتراف مطلوب")
        LocalDate confessionDate,

        String confessionFather,
        String notes
) {}
