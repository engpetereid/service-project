package org.serviceproject.classes.dto;

/**
 * Response DTO for class data enriched with secretary details and counts.
 */
public record GradeClassResponse(
        Long id,
        String name,
        Long ministryId,
        String ministryName,
        boolean active,
        int sortOrder,
        Long secretaryUserId,
        Long secretaryPersonId,
        String secretaryName,
        String secretaryPhone,
        long servantsCount,
        long studentsCount
) {
    public GradeClassResponse(Long id, String name, Long ministryId, String ministryName, boolean active, int sortOrder) {
        this(id, name, ministryId, ministryName, active, sortOrder, null, null, null, null, 0L, 0L);
    }
}
