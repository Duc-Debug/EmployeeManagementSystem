package com.hrm.employeemanagement.application.dto.project;

import com.hrm.employeemanagement.domain.project.ProjectMemberRole;

import java.time.LocalDate;

public record ProjectMemberResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        String email,
        Long orgUnitId,
        String orgUnitName,
        ProjectMemberRole roleInProject,
        String status,
        LocalDate contractEndDate
) {
    public ProjectMemberResult(
            Long employeeId,
            String employeeCode,
            String fullName,
            String email,
            Long orgUnitId,
            String orgUnitName,
            ProjectMemberRole roleInProject,
            String status
    ) {
        this(employeeId, employeeCode, fullName, email, orgUnitId, orgUnitName, roleInProject, status, null);
    }
}
