package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu mới phải có tối thiểu 8 ký tự")
        String newPassword,

        @NotBlank(message = "Mật khẩu xác nhận không được để trống")
        String confirmPassword
) {
}
