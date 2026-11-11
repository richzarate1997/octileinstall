package com.octileinstall.exception;

/**
 * Thrown when a caller exceeds a rate limit (currently: the per-IP limit on
 * {@code POST /api/public/inquiry} - see RateLimiterService). Mapped to 429 by
 * GlobalExceptionHandler.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
