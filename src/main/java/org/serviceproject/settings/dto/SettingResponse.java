package org.serviceproject.settings.dto;

import java.time.LocalDateTime;

/**
 * DTO representing an application system setting.
 */
public record SettingResponse(
        Long id,
        String key,
        String value,
        LocalDateTime updatedAt
) {}
