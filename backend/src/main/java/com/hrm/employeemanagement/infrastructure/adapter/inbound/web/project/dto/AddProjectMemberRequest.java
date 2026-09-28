package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import com.hrm.employeemanagement.application.dto.project.AddProjectMemberCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddProjectMemberRequest(
        @NotNull(message = "ID nhân viên không được để trống")
        @Positive(message = "ID nhân viên phải hợp lệ")
        Long employeeId
) {
    public AddProjectMemberCommand toCommand(Long projectId) {
        return new AddProjectMemberCommand(projectId, employeeId);
    }
}
