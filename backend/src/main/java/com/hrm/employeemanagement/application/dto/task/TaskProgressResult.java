package com.hrm.employeemanagement.application.dto.task;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.task.TaskStatus;

public record TaskProgressResult(
        Long taskId,
        Long projectId,
        String projectCode,
        String projectName,
        String taskCode,
        String name,
        TaskStatus previousStatus,
        TaskStatus currentStatus,
        LocalDateTime updatedAt
) {
}
