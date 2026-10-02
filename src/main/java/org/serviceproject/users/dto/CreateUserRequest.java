package org.serviceproject.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.serviceproject.users.entity.Gender;

import java.time.LocalDate;

/**
 * Request DTO for creating a new user (person + account).
 */
public record CreateUserRequest(
        @NotBlank(message = "الاسم بالكامل مطلوب")
        String fullName,

        @NotBlank(message = "رقم الهاتف مطلوب")
        @Size(max = 20, message = "رقم الهاتف يجب ألا يتجاوز 20 رقم")
        String phone,

        @NotBlank(message = "كلمة المرور مطلوبة")
        @Size(min = 6, message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
        String password,

        @NotNull(message = "الجنس مطلوب")
        Gender gender,

        LocalDate dateOfBirth,
        String address,
        String confessionFather
) {}
