package com.hrm.employeemanagement.application.dto.task;

import com.hrm.employeemanagement.domain.task.TaskStatus;

public record UpdateTaskProgressCommand(
        Long taskId,
        TaskStatus status
) {
}
