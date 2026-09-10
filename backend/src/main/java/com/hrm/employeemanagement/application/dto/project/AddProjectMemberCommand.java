package com.hrm.employeemanagement.application.dto.project;

public record AddProjectMemberCommand(
        Long projectId,
        Long employeeId
) {}
