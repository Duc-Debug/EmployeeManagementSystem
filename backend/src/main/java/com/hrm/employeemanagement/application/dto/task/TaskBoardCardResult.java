package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.domain.task.TaskStatus;

public record TaskBoardCardResult(
        Long taskId,
        String taskCode,
        String name,
        String description,
        Long projectId,
        String projectCode,
        String projectName,
        TaskStatus status,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        Integer sortOrder,
        List<TaskBoardAssigneeResult> assignees,
        boolean canMove
) {
}
