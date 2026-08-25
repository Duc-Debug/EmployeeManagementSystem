package com.hrm.employeemanagement.application.dto.user;

public record LogoutCommand(
        String token,
        Long userId,
        String username
) {
    public LogoutCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống khi thực hiện đăng xuất");
        }
    }

    public static LogoutCommand of(String token) {
        return new LogoutCommand(token, null, null);
    }

    public static LogoutCommand of(String token, Long userId, String username) {
        return new LogoutCommand(token, userId, username);
    }
}
