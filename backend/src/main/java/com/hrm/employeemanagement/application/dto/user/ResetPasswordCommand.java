package com.hrm.employeemanagement.application.dto.user;

public record ResetPasswordCommand(
        String token,
        String newPassword,
        String confirmPassword
) {
}
