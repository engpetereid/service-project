package org.serviceproject.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApiError} record.
 */
class ApiErrorTest {

    @Test
    void simpleConstructor_setsFieldsCorrectly() {
        ApiError error = new ApiError(400, "BAD_REQUEST", "Invalid input", "/api/test");

        assertEquals(400, error.status());
        assertEquals("BAD_REQUEST", error.code());
        assertEquals("Invalid input", error.message());
        assertEquals("/api/test", error.path());
        assertNull(error.fieldErrors());
        assertNotNull(error.timestamp());
    }

    @Test
    void validationConstructor_includesFieldErrors() {
        Map<String, String> fieldErrors = Map.of(
                "name", "Name is required",
                "phone", "Phone is invalid"
        );

        ApiError error = new ApiError(400, "VALIDATION_ERROR", "Validation failed",
                "/api/students", fieldErrors);

        assertEquals(400, error.status());
        assertEquals("VALIDATION_ERROR", error.code());
        assertNotNull(error.fieldErrors());
        assertEquals(2, error.fieldErrors().size());
        assertEquals("Name is required", error.fieldErrors().get("name"));
    }

    @Test
    void fullConstructor_setsAllFields() {
        var now = java.time.LocalDateTime.now();
        var fieldErrors = Map.of("field", "error");

        ApiError error = new ApiError(now, 422, "UNPROCESSABLE", "Cannot process",
                "/api/endpoint", fieldErrors);

        assertEquals(now, error.timestamp());
        assertEquals(422, error.status());
        assertEquals("UNPROCESSABLE", error.code());
        assertEquals("Cannot process", error.message());
        assertEquals("/api/endpoint", error.path());
        assertEquals(fieldErrors, error.fieldErrors());
    }
}
