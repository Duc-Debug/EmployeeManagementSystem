package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @ValidPassword(message = "Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số")
        String newPassword,

        @NotBlank(message = "Mật khẩu xác nhận không được để trống")
        String confirmPassword
) {
}
