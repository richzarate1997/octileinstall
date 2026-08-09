package com.octileinstall.exception;

/** Thrown for business-rule failures (e.g. a scheduled date in the past) that Bean Validation can't express. Mapped to 400 by each controller for now; Phase 6's @ControllerAdvice will centralize this. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
