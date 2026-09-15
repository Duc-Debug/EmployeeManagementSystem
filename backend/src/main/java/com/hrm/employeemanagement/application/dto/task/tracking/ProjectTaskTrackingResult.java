package com.hrm.employeemanagement.application.dto.task.tracking;

import java.math.BigDecimal;
import java.util.List;

import com.hrm.employeemanagement.domain.project.ProjectStatus;

/**
 * Kết quả tổng thể bảng theo dõi công việc của dự án (NCL-04-CN-003).
 */
public record ProjectTaskTrackingResult(
        Long projectId,
        String projectCode,
        String projectName,
        ProjectStatus projectStatus,
        int totalTasks,
        int overdueTasks,
        int completedTasks,
        int inProgressTasks,
        BigDecimal totalBudgetHours,
        BigDecimal totalActualHours,
        String suggestionMessage,
        List<TaskTrackingItemResult> tasks
) {
}
