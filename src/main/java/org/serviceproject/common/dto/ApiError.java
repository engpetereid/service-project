package org.serviceproject.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API error response structure.
 * <p>
 * All error responses from the application use this format for consistency.
 * {@code fieldErrors} is only included when present (validation errors).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    /** Convenience constructor without field errors. */
    public ApiError(int status, String code, String message, String path) {
        this(LocalDateTime.now(), status, code, message, path, null);
    }

    /** Convenience constructor with field errors (validation). */
    public ApiError(int status, String code, String message, String path,
                    Map<String, String> fieldErrors) {
        this(LocalDateTime.now(), status, code, message, path, fieldErrors);
    }
}
