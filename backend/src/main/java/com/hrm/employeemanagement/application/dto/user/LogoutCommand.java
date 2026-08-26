package com.hrm.employeemanagement.application.dto.user;

public record LogoutCommand(
        String token,
        boolean allDevices
) {
    public LogoutCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống khi thực hiện đăng xuất");
        }
    }

    public LogoutCommand(String token) {
        this(token, false);
    }

    public static LogoutCommand of(String token) {
        return new LogoutCommand(token, false);
    }

    public static LogoutCommand of(String token, boolean allDevices) {
        return new LogoutCommand(token, allDevices);
    }

    public static LogoutCommand of(String token, Long userId, String username) {
        return new LogoutCommand(token, false);
    }

    public static LogoutCommand of(String token, Long userId, String username, boolean allDevices) {
        return new LogoutCommand(token, allDevices);
    }
}

