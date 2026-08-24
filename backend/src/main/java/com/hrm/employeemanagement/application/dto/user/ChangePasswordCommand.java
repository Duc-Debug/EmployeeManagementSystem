package com.hrm.employeemanagement.application.dto.user;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
