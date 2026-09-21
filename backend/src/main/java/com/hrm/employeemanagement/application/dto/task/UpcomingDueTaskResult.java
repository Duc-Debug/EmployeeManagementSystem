package com.hrm.employeemanagement.application.dto.task;

import java.time.LocalDate;

/**
 * DTO đại diện cho một công việc sắp đến hạn dành cho Nhân viên chuyên môn (VT-04).
 */
public record UpcomingDueTaskResult(
        Long taskId,
        Long projectId,
        String taskCode,
        String taskName,
        LocalDate dueDate,
        long daysRemaining,
        String status,
        String directUrl
) {
}
