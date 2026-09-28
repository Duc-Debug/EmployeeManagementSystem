package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp,
        Object details) {

    public ErrorResponse(String code, String message, int status, LocalDateTime timestamp) {
        this(code, message, status, timestamp, null);
    }

    public static ErrorResponse of(String code, String message, int status) {
        return new ErrorResponse(code, message, status, LocalDateTime.now(), null);
    }

    public static ErrorResponse of(String code, String message, int status, Object details) {
        return new ErrorResponse(code, message, status, LocalDateTime.now(), details);
    }
}