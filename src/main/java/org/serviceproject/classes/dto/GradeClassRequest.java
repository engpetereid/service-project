package org.serviceproject.classes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a class.
 */
public record GradeClassRequest(
        @NotBlank(message = "اسم الفصل مطلوب")
        @Size(max = 100, message = "اسم الفصل يجب ألا يتجاوز 100 حرف")
        String name,

        @NotNull(message = "الخدمة مطلوبة")
        Long ministryId,

        int sortOrder
) {}
