package org.serviceproject.students.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload for student data including placement details.
 */
public record StudentResponse(
        Long personId,
        String fullName,
        String phone,
        String gender,
        LocalDate dateOfBirth,
        String address,
        String confessionFather,
        Long ministryId,
        String ministryName,
        Long classId,
        String className,
        Long servantId,
        String servantName,
        Long academicYearId,
        String academicYearName,
        String status,
        String guardianPhone,
        String talents,
        String additionalDetails,
        LocalDateTime assignedAt,
        boolean active
) {
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Long id() {
        return personId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("responsibleServantId")
    public Long responsibleServantId() {
        return servantId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("responsibleServantName")
    public String responsibleServantName() {
        return servantName;
    }
}
