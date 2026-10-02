package org.serviceproject.confession.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateConfessionSessionRequest(
        @NotNull(message = "تاريخ الجلسة مطلوب")
        LocalDate sessionDate,

        @NotBlank(message = "اسم الأب الكاهن مطلوب")
        String confessionFather,

        @NotEmpty(message = "يجب اختيار مخدوم واحد على الأقل في الجلسة")
        List<Long> studentIds,

        String notes
) {
}
