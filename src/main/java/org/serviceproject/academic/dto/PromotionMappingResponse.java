package org.serviceproject.academic.dto;

/**
 * Response DTO for a promotion mapping.
 */
public record PromotionMappingResponse(
        Long id,
        Long sourceClassId,
        String sourceClassName,
        String sourceMinistryName,
        Long targetClassId,
        String targetClassName,
        String targetMinistryName,
        boolean graduation
) {}
