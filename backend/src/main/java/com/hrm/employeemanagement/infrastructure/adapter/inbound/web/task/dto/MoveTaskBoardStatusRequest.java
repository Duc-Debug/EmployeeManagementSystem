package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import com.hrm.employeemanagement.application.dto.task.MoveTaskBoardStatusCommand;
import com.hrm.employeemanagement.domain.task.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record MoveTaskBoardStatusRequest(
        @NotNull(message = "Trạng thái mới không được để trống")
        TaskStatus newStatus
) {
    public MoveTaskBoardStatusCommand toCommand(Long taskId) {
        return new MoveTaskBoardStatusCommand(taskId, newStatus);
    }
}
