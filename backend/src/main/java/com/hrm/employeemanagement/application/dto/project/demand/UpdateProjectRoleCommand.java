package com.hrm.employeemanagement.application.dto.project.demand;

public record UpdateProjectRoleCommand(
        Long id,
        String name,
        String description,
        Long skillGroupId
) {}
