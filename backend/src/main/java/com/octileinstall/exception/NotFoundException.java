package com.octileinstall.exception;

/** Thrown when a requested entity id doesn't exist. Mapped to 404 by each controller for now; Phase 6's @ControllerAdvice will centralize this. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
