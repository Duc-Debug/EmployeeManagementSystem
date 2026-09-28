package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MyAllocationsErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        int status,
        @JsonProperty("error_code")
        String errorCode,
        String message
) {
    public static MyAllocationsErrorResponse of(int status, String errorCode, String message) {
        return new MyAllocationsErrorResponse(Instant.now(), status, errorCode, message);
    }
}