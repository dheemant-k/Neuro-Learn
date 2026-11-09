package com.neurolearn.exception;

/** Requested resource does not exist. Mapped to HTTP 404 by {@link GlobalExceptionHandler}. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
