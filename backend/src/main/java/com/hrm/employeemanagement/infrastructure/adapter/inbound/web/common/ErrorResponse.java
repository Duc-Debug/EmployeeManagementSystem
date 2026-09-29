package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ErrorResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("code") String code,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("message") String message,
        @JsonProperty("status") int status,
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("details") Object details) {

    public ErrorResponse(String code, String message, int status, LocalDateTime timestamp, Object details) {
        this(false, code, code, message, status, timestamp, details);
    }

    public ErrorResponse(String code, String message, int status, LocalDateTime timestamp) {
        this(false, code, code, message, status, timestamp, null);
    }

    public static ErrorResponse of(String code, String message, int status) {
        return new ErrorResponse(code, message, status, LocalDateTime.now(), null);
    }

    public static ErrorResponse of(String code, String message, int status, Object details) {
        return new ErrorResponse(code, message, status, LocalDateTime.now(), details);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Object getDetails() {
        return details;
    }
}