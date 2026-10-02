package org.serviceproject.attendance.dto;

import java.util.List;

/**
 * Detailed session response with all student attendance records.
 */
public record SessionDetailResponse(
        AttendanceSessionResponse session,
        List<AttendanceRecordResponse> records
) {}
