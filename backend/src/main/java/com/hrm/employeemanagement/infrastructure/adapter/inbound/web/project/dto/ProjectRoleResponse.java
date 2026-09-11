package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;

public record ProjectRoleResponse(
        Long id,
        String code,
        String name,
        String description
) {
    public static ProjectRoleResponse fromResult(ProjectRoleResult result) {
        if (result == null) return null;
        return new ProjectRoleResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description()
        );
    }
}
