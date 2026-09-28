package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import com.hrm.employeemanagement.application.dto.user.RoleResult;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description
) {
    public static RoleResponse fromResult(RoleResult result) {
        if (result == null) return null;
        return new RoleResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description()
        );
    }
}
