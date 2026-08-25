package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Tên đăng nhập hoặc Email không được để trống")
        String identity
) {
}
