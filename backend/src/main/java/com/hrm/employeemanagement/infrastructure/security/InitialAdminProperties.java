package com.hrm.employeemanagement.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.initial-admin")
public record InitialAdminProperties(
        boolean enabled,
        String username,
        String password
) {
}