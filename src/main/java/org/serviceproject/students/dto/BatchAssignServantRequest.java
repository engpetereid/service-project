package org.serviceproject.students.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request payload for assigning a responsible servant to multiple students in bulk.
 */
public record BatchAssignServantRequest(
        @NotEmpty(message = "يجب تحديد مخدوم واحد على الأقل")
        List<Long> studentIds,
        Long servantId
) {}
