package com.neurolearn.exception;

import java.time.OffsetDateTime;

/**
 * Uniform JSON error body. The frontend reads {@code message}.
 */
public record ApiError(int status, String error, String message, String path, OffsetDateTime timestamp) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, OffsetDateTime.now());
    }
}
