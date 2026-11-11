package com.octileinstall.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place every controller's error response goes through, replacing the local
 * @ExceptionHandler methods each controller carried through its own phase (Phase 3's
 * AuthController, Phase 4's InquiryController, Phase 5's InspectionController all said
 * "Phase 6 will absorb this" - this is that).
 *
 * <p>Every response here shares one shape: {status, error, message}, so a frontend can
 * handle errors generically instead of special-casing each endpoint.
 *
 * <p>Deliberately NOT handled here: Spring Security's AuthenticationException /
 * AccessDeniedException. Those are raised inside the security filter chain, which wraps
 * DispatcherServlet - by the time a request reaches a @Controller method, JwtAuthFilter has
 * already decided the outcome, and SecurityConfig's authenticationEntryPoint /
 * accessDeniedHandler produce the 401/403 response before this class is ever consulted. If
 * method security (@EnableMethodSecurity is on, though nothing uses @PreAuthorize yet) is
 * ever added to a controller method, an AccessDeniedException thrown from inside that method
 * body WOULD reach here first via Spring MVC's own exception resolution - and the generic
 * Exception handler below would swallow it into a misleading 500 instead of a 403. Add an
 * explicit rethrowing handler for AccessDeniedException/AuthenticationException at that point;
 * not added preemptively since no code path can trigger it today.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    /** @Valid failures on request DTOs (e.g. blank customerName, invalid email). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Malformed JSON, wrong field type, or an unrecognized enum value in a request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    /** A query/path param that doesn't convert to its target type (e.g. ?status=BOGUS, ?limit=abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'");
    }

    /** Safety net. The message is deliberately generic - anything unhandled by name is by definition unexpected, and its real detail belongs in the log, not the response. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
