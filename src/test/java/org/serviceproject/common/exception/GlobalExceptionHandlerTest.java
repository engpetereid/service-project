package org.serviceproject.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.serviceproject.common.dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleAppException_returnsMappedStatusAndCode() {
        AppException ex = AppException.notFound("STUDENT_NOT_FOUND", "Student not found");

        ResponseEntity<ApiError> response = handler.handleAppException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("STUDENT_NOT_FOUND", response.getBody().code());
        assertEquals("Student not found", response.getBody().message());
        assertEquals("/api/test", response.getBody().path());
    }

    @Test
    void handleAppException_badRequest() {
        AppException ex = AppException.badRequest("INVALID_SCORE", "Score out of range");

        ResponseEntity<ApiError> response = handler.handleAppException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void handleValidationException_returnsFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("student", "name", "Name is required");
        FieldError fieldError2 = new FieldError("student", "phone", "Phone is invalid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertNotNull(response.getBody().fieldErrors());
        assertEquals(2, response.getBody().fieldErrors().size());
        assertEquals("Name is required", response.getBody().fieldErrors().get("name"));
        assertEquals("Phone is invalid", response.getBody().fieldErrors().get("phone"));
    }

    @Test
    void handleAccessDeniedException_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Denied");

        ResponseEntity<ApiError> response = handler.handleAccessDeniedException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().code());
    }

    @Test
    void handleDataIntegrityViolation_returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry", new RuntimeException("Duplicate key"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATA_CONFLICT", response.getBody().code());
    }

    @Test
    void handleGenericException_returns500_noStackTrace() {
        RuntimeException ex = new RuntimeException("Something broke");

        ResponseEntity<ApiError> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("An unexpected error occurred", response.getBody().message());
        // Verify the actual exception message is NOT leaked to the client
        assertNotEquals("Something broke", response.getBody().message());
    }
}
