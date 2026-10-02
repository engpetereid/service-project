package org.serviceproject.ministries.dto;

/**
 * Response DTO for ministry data enriched with secretary details and system metrics.
 */
public record MinistryResponse(
        Long id,
        String name,
        boolean active,
        Long secretaryUserId,
        Long secretaryPersonId,
        String secretaryName,
        String secretaryPhone,
        long classesCount,
        long servantsCount,
        long studentsCount
) {
    public MinistryResponse(Long id, String name, boolean active) {
        this(id, name, active, null, null, null, null, 0L, 0L, 0L);
    }
}
