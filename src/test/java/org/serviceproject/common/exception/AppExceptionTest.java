package org.serviceproject.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AppException} factory methods.
 */
class AppExceptionTest {

    @Test
    void badRequest_createsExceptionWith400() {
        AppException ex = AppException.badRequest("INVALID_INPUT", "Bad input");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_INPUT", ex.getCode());
        assertEquals("Bad input", ex.getMessage());
    }

    @Test
    void notFound_createsExceptionWith404() {
        AppException ex = AppException.notFound("NOT_FOUND", "Resource not found");

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("NOT_FOUND", ex.getCode());
        assertEquals("Resource not found", ex.getMessage());
    }

    @Test
    void forbidden_createsExceptionWith403() {
        AppException ex = AppException.forbidden("FORBIDDEN", "Access denied");

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void conflict_createsExceptionWith409() {
        AppException ex = AppException.conflict("DUPLICATE", "Already exists");

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("DUPLICATE", ex.getCode());
    }

    @Test
    void unauthorized_createsExceptionWith401() {
        AppException ex = AppException.unauthorized("AUTH_FAILED", "Invalid credentials");

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("AUTH_FAILED", ex.getCode());
    }

    @Test
    void constructor_setsAllFields() {
        AppException ex = new AppException(HttpStatus.I_AM_A_TEAPOT, "TEAPOT", "I'm a teapot");

        assertEquals(HttpStatus.I_AM_A_TEAPOT, ex.getStatus());
        assertEquals("TEAPOT", ex.getCode());
        assertEquals("I'm a teapot", ex.getMessage());
    }
}
