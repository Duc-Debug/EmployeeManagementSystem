package com.hrm.employeemanagement.application.dto.project;

public record RemoveProjectMemberCommand(
        Long projectId,
        Long employeeId
) {}
