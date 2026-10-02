package org.serviceproject.reports.dto;

import org.serviceproject.attendance.entity.ActivityType;

/**
 * Filter parameters for attendance sessions report export.
 */
public record AttendanceReportFilter(
        Long weekId,
        ActivityType activityType,
        Long ministryId,
        Long classId
) {}
