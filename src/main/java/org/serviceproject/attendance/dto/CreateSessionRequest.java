package org.serviceproject.attendance.dto;

import jakarta.validation.constraints.NotNull;
import org.serviceproject.attendance.entity.ActivityType;

import java.time.LocalDate;

/**
 * Request payload for creating an attendance session.
 */
public record CreateSessionRequest(
        @NotNull(message = "الأسبوع مطلوب")
        Long weekId,

        @NotNull(message = "نوع النشاط مطلوب")
        ActivityType activityType,

        @NotNull(message = "تاريخ النشاط مطلوب")
        LocalDate sessionDate
) {}
