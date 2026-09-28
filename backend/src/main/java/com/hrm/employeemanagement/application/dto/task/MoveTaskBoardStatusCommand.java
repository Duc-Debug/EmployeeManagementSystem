package com.hrm.employeemanagement.application.dto.task;

import com.hrm.employeemanagement.domain.task.TaskStatus;

public record MoveTaskBoardStatusCommand(
        Long taskId,
        TaskStatus newStatus
) {
}
