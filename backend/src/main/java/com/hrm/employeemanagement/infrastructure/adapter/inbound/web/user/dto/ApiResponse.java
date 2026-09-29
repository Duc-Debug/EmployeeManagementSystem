package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String errorCode;
    private Integer status;
    private String message;
    private T data;
    private Object details;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, T data) {
        this(success, success ? "SUCCESS" : "ERROR", success ? null : "ERROR", success ? 200 : 400, message, data, null);
    }

    public ApiResponse(boolean success, String errorCode, String message, T data) {
        this(success, success ? "SUCCESS" : errorCode, errorCode, success ? 200 : 400, message, data, null);
    }

    public ApiResponse(boolean success, String code, String errorCode, Integer status, String message, T data, Object details) {
        this.success = success;
        this.code = code != null ? code : (success ? "SUCCESS" : errorCode);
        this.errorCode = errorCode != null ? errorCode : (success ? null : code);
        this.status = status != null ? status : (success ? 200 : 400);
        this.message = message;
        this.data = data;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Success Factories
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", null, 200, "Thành công", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", null, 200, message, data, null);
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", null, status, message, data, null);
    }

    // Error Factories
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, "ERROR", "ERROR", 400, message, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, errorCode, errorCode, 400, message, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, int status) {
        return new ApiResponse<>(false, errorCode, errorCode, status, message, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, int status, Object details) {
        return new ApiResponse<>(false, errorCode, errorCode, status, message, null, details);
    }

    // Getters & Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        if (code != null) {
            return code;
        }
        return success ? "SUCCESS" : errorCode;
    }

    public void setCode(String code) {
        this.code = code;
        if (this.errorCode == null && !this.success) {
            this.errorCode = code;
        }
    }

    public String getErrorCode() {
        if (success) {
            return null;
        }
        return errorCode != null ? errorCode : code;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        if (this.code == null && !this.success) {
            this.code = errorCode;
        }
    }

    public Integer getStatus() {
        if (status != null) {
            return status;
        }
        return success ? 200 : 400;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
