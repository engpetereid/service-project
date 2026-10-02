package org.serviceproject.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.serviceproject.users.entity.Gender;

import java.time.LocalDate;

/**
 * Request DTO for updating user (person data only — password and roles are separate).
 */
public record UpdateUserRequest(
        @NotBlank(message = "الاسم بالكامل مطلوب")
        String fullName,

        @NotBlank(message = "رقم الهاتف مطلوب")
        @Size(max = 20, message = "رقم الهاتف يجب ألا يتجاوز 20 رقم")
        String phone,

        @NotNull(message = "الجنس مطلوب")
        Gender gender,

        LocalDate dateOfBirth,
        String address,
        String confessionFather
) {}
