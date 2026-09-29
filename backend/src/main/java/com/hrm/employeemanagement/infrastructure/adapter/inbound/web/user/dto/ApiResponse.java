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
        this(success, null, null, null, message, data, null);
    }

    public ApiResponse(boolean success, String errorCode, String message, T data) {
        this(success, errorCode, errorCode, null, message, data, null);
    }

    public ApiResponse(boolean success, String code, String errorCode, Integer status, String message, T data, Object details) {
        this.success = success;
        this.code = code;
        this.errorCode = errorCode != null ? errorCode : code;
        this.status = status;
        this.message = message;
        this.data = data;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Success Factories
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, "Thành công", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, null, message, data);
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
        return code != null ? code : errorCode;
    }

    public void setCode(String code) {
        this.code = code;
        if (this.errorCode == null) {
            this.errorCode = code;
        }
    }

    public String getErrorCode() {
        return errorCode != null ? errorCode : code;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        if (this.code == null) {
            this.code = errorCode;
        }
    }

    public Integer getStatus() {
        return status;
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
