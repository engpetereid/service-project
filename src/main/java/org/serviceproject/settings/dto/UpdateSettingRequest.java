package org.serviceproject.settings.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating an existing system setting.
 */
public record UpdateSettingRequest(
        @NotBlank(message = "قيمة الإعداد مطلوبة")
        String value
) {}
