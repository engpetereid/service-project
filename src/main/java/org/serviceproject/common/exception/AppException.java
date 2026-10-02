package org.serviceproject.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Application-level exception that carries an HTTP status and error code.
 * <p>
 * Caught by {@link GlobalExceptionHandler} and mapped to a consistent
 * {@link org.serviceproject.common.dto.ApiError} response.
 * <p>
 * Factory methods provide a clean API for common HTTP error scenarios.
 */
@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    // ── Factory methods ──────────────────────────────────────────────

    public static AppException badRequest(String code, String message) {
        return new AppException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static AppException notFound(String code, String message) {
        return new AppException(HttpStatus.NOT_FOUND, code, message);
    }

    public static AppException forbidden(String code, String message) {
        return new AppException(HttpStatus.FORBIDDEN, code, message);
    }

    public static AppException conflict(String code, String message) {
        return new AppException(HttpStatus.CONFLICT, code, message);
    }

    public static AppException unauthorized(String code, String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, code, message);
    }
}
