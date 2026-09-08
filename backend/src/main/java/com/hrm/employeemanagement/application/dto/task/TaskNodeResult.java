package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;

/**
 * Đại diện cho cấu trúc cây WBS (Tree node) có danh sách các node con children
 * lồng nhau.
 */
public record TaskNodeResult(
        Long id,
        Long projectId,
        Long parentId,
        String taskCode,
        String name,
        String description,
        TaskType taskType,
        Long assigneeId,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        TaskStatus status,
        Integer sortOrder,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version,
        List<TaskNodeResult> children) {
    public TaskNodeResult {
        if (children == null) {
            children = new ArrayList<>();
        }
    }

    public static TaskNodeResult from(TaskResult task) {
        return new TaskNodeResult(task.id(),
                task.projectId(),
                task.parentId(),
                task.taskCode(),
                task.name(),
                task.description(),
                task.taskType(),
                task.assigneeId(),
                task.estimatedHours(),
                task.actualHours(),
                task.status(),
                task.sortOrder(),
                task.createdBy(),
                task.createdAt(),
                task.updatedAt(),
                task.version(),
                new ArrayList<>());
    }
}
