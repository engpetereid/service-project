package org.serviceproject.students.dto;

/**
 * Request payload for reassigning or unassigning a student's responsible servant.
 */
public record ChangeAssignmentRequest(
        Long servantId
) {}
