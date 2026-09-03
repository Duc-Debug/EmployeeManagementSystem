package com.hrm.employeemanagement.application.dto.user;

public record RoleResult(
        Long id,
        String code,
        String name,
        String description
) {
    public RoleResult(Long id, String code, String name) {
        this(id, code, name, null);
    }
}
