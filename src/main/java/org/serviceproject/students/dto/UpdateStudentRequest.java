package org.serviceproject.students.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.serviceproject.users.entity.Gender;

import java.time.LocalDate;

/**
 * Request payload for updating student data and placement.
 * Note: Changing ministryId and classId is restricted to GENERAL_ADMIN.
 */
public record UpdateStudentRequest(
        @NotBlank(message = "اسم المخدوم مطلوب")
        String fullName,

        @Size(max = 20, message = "رقم الهاتف يجب ألا يتجاوز 20 رقم")
        String phone,

        @NotNull(message = "الجنس مطلوب")
        Gender gender,

        LocalDate dateOfBirth,
        String address,
        String confessionFather,

        Long ministryId,
        Long classId,
        String guardianPhone,
        String talents,
        String additionalDetails
) {}
