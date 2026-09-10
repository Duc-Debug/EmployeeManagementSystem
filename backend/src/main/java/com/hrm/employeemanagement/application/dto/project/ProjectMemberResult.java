package com.hrm.employeemanagement.application.dto.project;

import com.hrm.employeemanagement.domain.project.ProjectMemberRole;

public record ProjectMemberResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        String email,
        Long orgUnitId,
        String orgUnitName,
        ProjectMemberRole roleInProject,
        String status
) {}
