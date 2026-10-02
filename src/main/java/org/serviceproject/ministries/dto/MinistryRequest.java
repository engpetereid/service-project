package org.serviceproject.ministries.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a ministry.
 */
public record MinistryRequest(
        @NotBlank(message = "اسم الخدمة مطلوب")
        @Size(max = 100, message = "اسم الخدمة يجب ألا يتجاوز 100 حرف")
        String name
) {}
