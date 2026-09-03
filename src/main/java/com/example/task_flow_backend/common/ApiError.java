package com.example.task_flow_backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape every endpoint returns (plan section 3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors,
        String path) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, null, path);
    }

    public static ApiError of(int status, String code, String message,
                              Map<String, String> fieldErrors, String path) {
        return new ApiError(Instant.now(), status, code, message, fieldErrors, path);
    }
}
