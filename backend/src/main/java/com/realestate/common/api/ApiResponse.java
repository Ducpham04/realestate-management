package com.realestate.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Unified response envelope returned by every endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode,
        List<FieldViolation> errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, null, errorCode, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, List<FieldViolation> errors) {
        return new ApiResponse<>(false, message, null, errorCode, errors, Instant.now());
    }

    public record FieldViolation(String field, String message) {
    }
}
