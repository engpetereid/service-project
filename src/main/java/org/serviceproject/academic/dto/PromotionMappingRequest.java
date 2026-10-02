package org.serviceproject.academic.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a promotion mapping.
 */
public record PromotionMappingRequest(
        @NotNull(message = "فصل البداية مطلوب")
        Long sourceClassId,

        Long targetClassId,

        boolean graduation
) {}
