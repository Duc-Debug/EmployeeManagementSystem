package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp) {
    public static ErrorResponse of(String code, String message, int status) {
        return new ErrorResponse(code, message, status, LocalDateTime.now());
    }
}