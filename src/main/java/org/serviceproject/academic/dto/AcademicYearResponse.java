package org.serviceproject.academic.dto;

import java.time.LocalDate;

/**
 * Response DTO for academic year data.
 */
public record AcademicYearResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean current
) {}
