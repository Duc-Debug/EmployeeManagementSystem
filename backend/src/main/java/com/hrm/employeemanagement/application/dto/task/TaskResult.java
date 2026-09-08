package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
/**
 * Trả về dữ liệu phẳng (flat) của một task sau khi tạo hoặc truy vấn.
 */
public record TaskResult(
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
        Long version
) {
    
}
