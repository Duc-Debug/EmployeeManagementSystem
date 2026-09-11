package com.hrm.employeemanagement.application.dto.project.demand;

public record CreateProjectRoleCommand(
        String code,
        String name,
        String description,
        Long skillGroupId
) {}
