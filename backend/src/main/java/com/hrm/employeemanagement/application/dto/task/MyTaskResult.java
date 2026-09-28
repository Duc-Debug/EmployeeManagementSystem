package com.hrm.employeemanagement.application.dto.task;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hrm.employeemanagement.domain.task.TaskStatus;

public record MyTaskResult(
        Long taskId,
        Long projectId,
        String projectCode,
        String projectName,
        String taskCode,
        String taskName,
        TaskStatus status,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        boolean isPrimary
) {}

