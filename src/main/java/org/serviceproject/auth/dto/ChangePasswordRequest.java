package org.serviceproject.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for changing the current user's password.
 * Backend verifies the old password first (requirement #5.1).
 */
public record ChangePasswordRequest(
        @NotBlank(message = "كلمة المرور الحالية مطلوبة")
        String oldPassword,

        @NotBlank(message = "كلمة المرور الجديدة مطلوبة")
        @Size(min = 6, message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
        String newPassword
) {}
