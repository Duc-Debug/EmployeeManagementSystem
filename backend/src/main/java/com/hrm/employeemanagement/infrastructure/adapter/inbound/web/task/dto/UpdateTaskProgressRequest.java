package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import com.hrm.employeemanagement.application.dto.task.UpdateTaskProgressCommand;
import com.hrm.employeemanagement.domain.task.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTaskProgressRequest(
        @NotNull(message = "Trạng thái công việc không được để trống")
        TaskStatus status
) {
    public UpdateTaskProgressCommand toCommand(Long taskId) {
        return new UpdateTaskProgressCommand(taskId, status);
    }
}
