package org.serviceproject.students.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request payload for moving multiple students to a new ministry and grade class in bulk.
 */
public record BatchMoveClassRequest(
        @NotEmpty(message = "يجب تحديد مخدوم واحد على الأقل")
        List<Long> studentIds,
        @NotNull(message = "الخدمة مطلوبة")
        Long ministryId,
        @NotNull(message = "الفصل مطلوب")
        Long classId
) {}
