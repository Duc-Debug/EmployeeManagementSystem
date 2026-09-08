package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;

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
        Integer sortOrder) {
}
