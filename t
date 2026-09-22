warning: in the working copy of 'src/main/java/com/kangoute/appointment/exception/GlobalExceptionHandler.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/test/java/com/kangoute/appointment/AuthJwtIntegrationTests.java', LF will be replaced by CRLF the next time Git touches it
[1mdiff --git a/src/main/java/com/kangoute/appointment/exception/GlobalExceptionHandler.java b/src/main/java/com/kangoute/appointment/exception/GlobalExceptionHandler.java[m
[1mindex 526eca6..4523e90 100644[m
[1m--- a/src/main/java/com/kangoute/appointment/exception/GlobalExceptionHandler.java[m
[1m+++ b/src/main/java/com/kangoute/appointment/exception/GlobalExceptionHandler.java[m
[36m@@ -1,5 +1,7 @@[m
 package com.kangoute.appointment.exception;[m
 [m
[32m+[m[32mimport com.kangoute.appointment.dto.response.ApiErrorResponse;[m
[32m+[m[32mimport jakarta.servlet.http.HttpServletRequest;[m
 import lombok.extern.slf4j.Slf4j;[m
 import org.springframework.http.HttpStatus;[m
 import org.springframework.http.ResponseEntity;[m
[36m@@ -19,78 +21,83 @@[m [mimport java.util.Map;[m
 public class GlobalExceptionHandler {[m
 [m
     @ExceptionHandler(ResourceNotFoundException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {[m
[31m-        return ResponseEntity.status(HttpStatus.NOT_FOUND)[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(DuplicateResourceException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateResourceException ex) {[m
[31m-        return ResponseEntity.status(HttpStatus.CONFLICT)[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(InvalidAppointmentTimeException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleInvalidAppointmentTime(InvalidAppointmentTimeException ex) {[m
[31m-        return ResponseEntity.badRequest()[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleInvalidAppointmentTime(InvalidAppointmentTimeException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(AppointmentConflictException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleAppointmentConflict(AppointmentConflictException ex) {[m
[31m-        return ResponseEntity.status(HttpStatus.CONFLICT)[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleAppointmentConflict(AppointmentConflictException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(AppointmentOutsideAvailabilityException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleAppointmentOutsideAvailability(AppointmentOutsideAvailabilityException ex) {[m
[31m-        return ResponseEntity.badRequest()[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleAppointmentOutsideAvailability(AppointmentOutsideAvailabilityException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(InvalidStatisticsPeriodException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleInvalidStatisticsPeriod(InvalidStatisticsPeriodException ex) {[m
[31m-        return ResponseEntity.badRequest()[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleInvalidStatisticsPeriod(InvalidStatisticsPeriodException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(AccessDeniedException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {[m
[31m-        return ResponseEntity.status(HttpStatus.FORBIDDEN)[m
[31m-                .body(Map.of("message", ex.getMessage()));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);[m
     }[m
 [m
     @ExceptionHandler(AuthenticationException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {[m
         log.warn("Authentication failed: {}", ex.getClass().getSimpleName());[m
[31m-        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)[m
[31m-                .body(Map.of("message", "Email ou mot de passe incorrect"));[m
[32m+[m[32m        return buildResponse(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect", request);[m
     }[m
 [m
     @ExceptionHandler(HttpRequestMethodNotSupportedException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {[m
[31m-        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)[m
[31m-                .body(Map.of("message", "Methode HTTP non autorisee"));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Methode HTTP non autorisee", request);[m
     }[m
 [m
     @ExceptionHandler(HttpMessageNotReadableException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {[m
[31m-        return ResponseEntity.badRequest()[m
[31m-                .body(Map.of("message", "Requete JSON invalide"));[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {[m
[32m+[m[32m        return buildResponse(HttpStatus.BAD_REQUEST, "Requete JSON invalide", request);[m
     }[m
 [m
     @ExceptionHandler(MethodArgumentNotValidException.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {[m
         Map<String, String> errors = new HashMap<>();[m
         ex.getBindingResult().getFieldErrors()[m
                 .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));[m
[31m-        return ResponseEntity.badRequest().body(errors);[m
[32m+[m[32m        return buildResponse(HttpStatus.BAD_REQUEST, "Erreur de validation", request, errors);[m
     }[m
 [m
     @ExceptionHandler(Exception.class)[m
[31m-    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {[m
[32m+[m[32m    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {[m
         log.error("Unhandled application error", ex);[m
[31m-        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)[m
[31m-         