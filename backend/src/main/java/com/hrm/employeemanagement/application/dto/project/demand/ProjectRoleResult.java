package com.hrm.employeemanagement.application.dto.project.demand;

public record ProjectRoleResult(
        Long id,
        String code,
        String name,
        String description,
        Long skillGroupId,
        String skillGroupName,
        String status
) {
    public ProjectRoleResult(Long id, String code, String name, String description) {
        this(id, code, name, description, null, null, "ACTIVE");
    }
}
