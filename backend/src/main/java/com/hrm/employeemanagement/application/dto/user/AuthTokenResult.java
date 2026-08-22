package com.hrm.employeemanagement.application.dto.user;

public class AuthTokenResult {
    private final String token;
    private final String tokenType;
    private final Long userId;
    private final String username;
    private final String roleCode;

    public AuthTokenResult(String token, String tokenType, Long userId, String username, String roleCode) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRoleCode() {
        return roleCode;
    }
}
