package com.octileinstall.exception;

/** Thrown for authentication failures raised by application code (e.g. bad login credentials), as opposed to Spring Security's own filter-chain-level auth failures (missing/invalid token), which SecurityConfig handles separately. Mapped to 401 by GlobalExceptionHandler. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
