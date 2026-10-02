package org.serviceproject.staff.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Response payload for a servant including placement details.
 */
public record ServantResponse(
        Long personId,
        Long userId,
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
        Long academicYearId,
        String academicYearName,
        List<String> roles,
        boolean isClassSecretary,
        boolean active
) {
    public ServantResponse(
            Long personId,
            Long userId,
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
            Long academicYearId,
            String academicYearName,
            boolean active
    ) {
        this(personId, userId, fullName, phone, gender, dateOfBirth, address, confessionFather,
             ministryId, ministryName, classId, className, academicYearId, academicYearName,
             List.of(), false, active);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Long id() {
        return personId;
    }
}
