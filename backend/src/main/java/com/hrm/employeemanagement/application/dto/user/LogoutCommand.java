package com.hrm.employeemanagement.application.dto.user;

public record LogoutCommand(
        String token,
        Long userId,
        String username,
        boolean allDevices
) {
    public LogoutCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống khi thực hiện đăng xuất");
        }
    }

    public LogoutCommand(String token, Long userId, String username) {
        this(token, userId, username, false);
    }

    public static LogoutCommand of(String token) {
        return new LogoutCommand(token, null, null, false);
    }

    public static LogoutCommand of(String token, Long userId, String username) {
        return new LogoutCommand(token, userId, username, false);
    }

    public static LogoutCommand of(String token, Long userId, String username, boolean allDevices) {
        return new LogoutCommand(token, userId, username, allDevices);
    }
}

