package org.serviceproject.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.serviceproject.users.entity.Gender;

import java.time.LocalDate;

/**
 * Request payload for creating a servant and their placement.
 */
public record CreateServantRequest(
        @NotBlank(message = "اسم الخادم مطلوب")
        String fullName,

        @NotBlank(message = "رقم الهاتف مطلوب")
        @Size(max = 20, message = "رقم الهاتف يجب ألا يتجاوز 20 رقم")
        String phone,

        @NotNull(message = "الجنس مطلوب")
        Gender gender,

        LocalDate dateOfBirth,
        String address,
        String confessionFather,

        @NotNull(message = "الخدمة مطلوبة")
        Long ministryId,

        @NotNull(message = "الفصل مطلوب")
        Long classId,

        /** Optional initial password. If omitted, phone number is used as initial password. */
        String password
) {}
