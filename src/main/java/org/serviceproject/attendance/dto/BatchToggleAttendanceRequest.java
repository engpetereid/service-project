package org.serviceproject.attendance.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request payload for recording attendance for multiple students in bulk.
 */
public record BatchToggleAttendanceRequest(
        @NotNull(message = "الجلسة مطلوبة")
        Long sessionId,

        @NotEmpty(message = "يجب تحديد مخدوم واحد على الأقل")
        List<Long> studentIds,

        boolean present
) {}
