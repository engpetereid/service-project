package org.serviceproject.attendance.dto;

import java.time.LocalDateTime;

/**
 * Detailed response for a student attendance record.
 */
public record AttendanceRecordResponse(
        Long id,
        Long sessionId,
        Long studentId,
        String studentName,
        boolean present,
        Long recordedById,
        String recordedByName,
        LocalDateTime recordedAt
) {}
