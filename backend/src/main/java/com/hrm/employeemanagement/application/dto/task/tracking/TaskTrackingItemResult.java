package com.hrm.employeemanagement.application.dto.task.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.application.dto.task.TaskBoardAssigneeResult;
import com.hrm.employeemanagement.domain.task.TaskStatus;

/**
 * Chi tiết công việc trên bảng theo dõi của dự án (NCL-04-CN-003).
 */
public record TaskTrackingItemResult(
        Long taskId,
        String taskCode,
        String taskName,
        String description,
        Long categoryId,
        String categoryName,
        List<TaskBoardAssigneeResult> assignees,
        TaskStatus status,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        BigDecimal budgetHours,
        BigDecimal actualHours,
        BigDecimal burnedPercentage,
        boolean isOverdue,
        long overdueDays,
        int sortOrder
) {
}
