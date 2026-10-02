package org.serviceproject.search.dto;

/**
 * Result item returned by the global search endpoint.
 */
public record SearchResultResponse(
        Long personId,
        String fullName,
        String phone,
        String personType,
        Long ministryId,
        String ministryName,
        Long classId,
        String className,
        Long responsibleServantId,
        String responsibleServantName,
        String status
) {}
