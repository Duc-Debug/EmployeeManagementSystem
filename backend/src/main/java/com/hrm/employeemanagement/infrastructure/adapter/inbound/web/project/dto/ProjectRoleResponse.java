package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;

public record ProjectRoleResponse(
        Long id,
        String code,
        String name,
        String description,
        Long skillGroupId,
        String skillGroupName,
        String status
) {
    public ProjectRoleResponse(Long id, String code, String name, String description) {
        this(id, code, name, description, null, null, "ACTIVE");
    }

    public static ProjectRoleResponse fromResult(ProjectRoleResult result) {
        if (result == null) return null;
        return new ProjectRoleResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.skillGroupId(),
                result.skillGroupName(),
                result.status()
        );
    }
}
