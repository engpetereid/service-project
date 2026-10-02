package org.serviceproject.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for admin password reset.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "كلمة المرور الجديدة مطلوبة")
        @Size(min = 6, message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
        String newPassword
) {}

