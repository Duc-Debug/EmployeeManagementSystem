package com.hrm.employeemanagement.application.dto.user;

public record LoginCommand(
        String username,
        String password
) {
    public LoginCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
    }
}
