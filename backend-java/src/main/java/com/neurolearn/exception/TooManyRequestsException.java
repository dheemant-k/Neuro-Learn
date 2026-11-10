package com.neurolearn.exception;

/** A usage limit was hit (e.g. daily question generation). Mapped to HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
