package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsCommand;

import jakarta.validation.constraints.NotNull;

public record CloneProjectWbsRequest(
        @NotNull(message = "Mã dự án nguồn (sourceProjectId) không được để trống")
        Long sourceProjectId
        ) {

    public CloneProjectWbsCommand toCommand(Long targetProjectId) {
        return new CloneProjectWbsCommand(targetProjectId, this.sourceProjectId);
    }
}
