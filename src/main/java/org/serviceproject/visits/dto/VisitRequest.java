package org.serviceproject.visits.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.serviceproject.visits.entity.VisitMethod;

/**
 * Request payload for creating a visit or call record.
 */
public record VisitRequest(
        @NotNull(message = "المخدوم مطلوب")
        Long studentId,

        @NotNull(message = "الأسبوع مطلوب")
        Long weekId,

        @NotNull(message = "نوع الافتقاد مطلوب")
        VisitMethod method,

        @Min(value = 0, message = "درجة الصلاة يجب أن تكون بين 0 و 7")
        @Max(value = 7, message = "درجة الصلاة يجب أن تكون بين 0 و 7")
        Integer prayerScore,

        @Min(value = 0, message = "درجة القراءة يجب أن تكون بين 0 و 7")
        @Max(value = 7, message = "درجة القراءة يجب أن تكون بين 0 و 7")
        Integer readingScore,

        @Min(value = 0, message = "درجة النوتة لا يمكن أن تكون سالبة")
        Integer noteScore,

        String notes
) {}
