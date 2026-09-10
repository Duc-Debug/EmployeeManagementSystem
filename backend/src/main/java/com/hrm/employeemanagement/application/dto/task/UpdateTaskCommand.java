package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.task.TaskStatus;

/**
 * DTO mang dữ liệu cập nhật thông tin Hạng mục hoặc Công việc trong cây WBS.
 */
public record UpdateTaskCommand(
        Long projectId,
        Long taskId,
        Long parentId,
        String name,
        String description,
        Long assigneeId,
        BigDecimal estimatedHours,
        Integer sortOrder,
        TaskStatus status) {

    public UpdateTaskCommand(
            Long projectId,
            Long taskId,
            Long parentId,
            String name,
            String description,
            Long assigneeId,
            BigDecimal estimatedHours,
            Integer sortOrder) {
        this(projectId, taskId, parentId, name, description, assigneeId, estimatedHours, sortOrder, null);
    }
}
