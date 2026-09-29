package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @deprecated Toàn bộ xử lý ngoại lệ được tập trung vào {@link com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler} và sử dụng {@link com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse}.
 */
@Deprecated
public record MyAllocationsErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        int status,
        @JsonProperty("errorCode")
        String errorCode,
        String message
) {
    public static MyAllocationsErrorResponse of(int status, String errorCode, String message) {
        return new MyAllocationsErrorResponse(Instant.now(), status, errorCode, message);
    }
}