package com.prashant.api.ecom.ducart.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badRequest_shouldReturn400() {
        BadRequestException exception = new BadRequestException("Product is required");

        ResponseEntity<Map<String, Object>> response = handler.badRequest(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Product is required");
    }

    @Test
    void unauthorized_shouldReturn401() {
        UnauthorizedException exception = new UnauthorizedException("Authentication is required");

        ResponseEntity<Map<String, Object>> response = handler.unauthorized(exception);

        assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    @Test
    void notFound_shouldReturn404() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Product not found");

        ResponseEntity<Map<String, Object>> response = handler.notFound(exception);

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Product not found");
    }

    @Test
    void duplicate_shouldReturn409WithSafeMessage() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Database constraint details");

        ResponseEntity<Map<String, Object>> response = handler.duplicate(exception);

        assertErrorResponse(response, HttpStatus.CONFLICT, "Duplicate or invalid database value");
    }

    @Test
    void validation_whenFieldErrorExists_shouldReturn400WithFieldMessage() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "product");
        bindingResult.addError(new FieldError("product", "name", "Name is required"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.validation(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Name is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void validation_shouldReturnAllFieldErrorsWithRequiredMessageFirst() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "signup");
        bindingResult.addError(new FieldError("signup", "name", null, false,
                new String[] {"Size"}, null, "Name must be between 3 and 15 characters"));
        bindingResult.addError(new FieldError("signup", "name", null, false,
                new String[] {"NotBlank"}, null, "Name is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.validation(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Name is required");
        Map<String, List<String>> errors = (Map<String, List<String>>) response.getBody().get("errors");
        assertEquals(List.of("Name is required", "Name must be between 3 and 15 characters"), errors.get("name"));
    }

    @Test
    void validation_whenNoFieldError_shouldReturn400WithDefaultMessage() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "product");
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.validation(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @Test
    void conflict_shouldReturn409() {
        ConflictException exception = new ConflictException("Product already exists in wishlist");

        ResponseEntity<Map<String, Object>> response = handler.conflict(exception);

        assertErrorResponse(response, HttpStatus.CONFLICT, "Product already exists in wishlist");
    }

    @Test
    void badCredentials_shouldReturn401WithSafeMessage() {
        BadCredentialsException exception = new BadCredentialsException("Internal authentication details");

        ResponseEntity<Map<String, Object>> response = handler.badCredentials(exception);

        assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
    }

    @Test
    void accessDenied_shouldReturn403WithSafeMessage() {
        AccessDeniedException exception = new AccessDeniedException("Internal authorization details");

        ResponseEntity<Map<String, Object>> response = handler.accessDenied(exception);

        assertErrorResponse(response, HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource");
    }

    @Test
    void generalException_shouldReturn500() {
        Exception exception = new Exception("Unexpected server error");

        ResponseEntity<Map<String, Object>> response = handler.general(exception);

        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    private void assertErrorResponse(
            ResponseEntity<Map<String, Object>> response,
            HttpStatus expectedStatus,
            String expectedMessage) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertEquals(expectedStatus.value(), body.get("status"));
        assertEquals(expectedMessage, body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertDoesNotThrow(() -> LocalDateTime.parse((String) body.get("timestamp")));
    }

}
