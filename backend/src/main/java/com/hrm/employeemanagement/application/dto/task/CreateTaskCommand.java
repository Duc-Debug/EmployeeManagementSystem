package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.task.TaskType;
/**
 * Mang dữ liệu từ Use Case để tạo mới Hạng mục (CATEGORY) hoặc Công việc (TASK).
 */
public record CreateTaskCommand(
    Long projectId,
    Long parentId,
    String taskCode,
    String name,
    String description,
    TaskType taskType,
    Long assigneeId,
    BigDecimal estimatedHours,
    Integer sortOrder
) {
}
