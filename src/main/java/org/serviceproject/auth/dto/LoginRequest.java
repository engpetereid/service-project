package org.serviceproject.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload.
 * Username is the person's phone number (requirement #5).
 */
public record LoginRequest(
        @NotBlank(message = "رقم الهاتف مطلوب")
        String phone,

        @NotBlank(message = "كلمة المرور مطلوبة")
        String password
) {}
