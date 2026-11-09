package com.neurolearn.exception;

/**
 * Caller is authenticated but may not access this specific resource
 * (e.g. another student's session, an unlinked child). Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
