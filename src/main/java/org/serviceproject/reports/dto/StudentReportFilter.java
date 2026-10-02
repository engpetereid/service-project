package org.serviceproject.reports.dto;

import org.serviceproject.students.entity.StudentStatus;

/**
 * Filter parameters for student roster report export.
 */
public record StudentReportFilter(
        Long academicYearId,
        Long ministryId,
        Long classId,
        Long servantId,
        StudentStatus status
) {}
