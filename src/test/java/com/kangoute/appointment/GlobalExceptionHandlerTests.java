package com.kangoute.appointment;

import com.kangoute.appointment.dto.request.UserCreateRequest;
import com.kangoute.appointment.dto.response.ApiErrorResponse;
import com.kangoute.appointment.exception.AppointmentConflictException;
import com.kangoute.appointment.exception.GlobalExceptionHandler;
import com.kangoute.appointment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundResponseUsesStandardApiErrorBody() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Utilisateur introuvable"),
                request("/api/users/99")
        );

        assertError(response, HttpStatus.NOT_FOUND, "Utilisateur introuvable", "/api/users/99");
    }

    @Test
    void validationResponseContainsFieldErrors() throws Exception {
        MethodArgumentNotValidException exception = validationException("email", "must be a well-formed email address");

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request("/api/users"));

        assertError(response, HttpStatus.BAD_REQUEST, "Erreur de validation", "/api/users");
        assertEquals("must be a well-formed email address", response.getBody().getValidationErrors().get("email"));
    }

    @Test
    void accessDeniedResponseUsesForbiddenStatus() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("Acces refuse"),
                request("/api/admin/users")
        );

        assertError(response, HttpStatus.FORBIDDEN, "Acces refuse", "/api/admin/users");
    }

    @Test
    void conflictResponseUsesConflictStatus() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAppointmentConflict(
                new AppointmentConflictException("L'utilisateur a deja un rendez-vous sur ce creneau"),
                request("/api/appointments")
        );

        assertError(response, HttpStatus.CONFLICT, "L'utilisateur a deja un rendez-vous sur ce creneau", "/api/appointments");
    }

    @Test
    void bookingLockContentionReturnsSafeConflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBookingContention(
                new org.springframework.dao.CannotAcquireLockException("internal database details"),
                request("/api/appointments"));
        assertError(response, HttpStatus.CONFLICT,
                "Le calendrier est en cours de modification. Veuillez réessayer.", "/api/appointments");
    }

    @Test
    void unexpectedErrorResponseDoesNotExposeInternalExceptionDetails() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("SQL token password internal detail"),
                request("/api/test")
        );

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur inattendue", "/api/test");
    }

    private MethodArgumentNotValidException validationException(String field, String message) throws Exception {
        UserCreateRequest target = new UserCreateRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", field, message));

        Method method = GlobalExceptionHandlerTests.class.getDeclaredMethod("validationTarget", UserCreateRequest.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private void validationTarget(UserCreateRequest request) {
    }

    private MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private void assertError(ResponseEntity<ApiErrorResponse> response, HttpStatus status, String message, String path) {
        assertEquals(status, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(status.value(), response.getBody().getStatus());
        assertEquals(status.getReasonPhrase(), response.getBody().getError());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(path, response.getBody().getPath());
    }
}
