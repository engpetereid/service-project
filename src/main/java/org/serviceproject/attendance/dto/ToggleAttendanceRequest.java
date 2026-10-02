package org.serviceproject.attendance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for recording or toggling attendance for a student in a session.
 */
public record ToggleAttendanceRequest(
        @NotNull(message = "الجلسة مطلوبة")
        Long sessionId,

        @NotNull(message = "المخدوم مطلوب")
        Long studentId,

        boolean present
) {}
