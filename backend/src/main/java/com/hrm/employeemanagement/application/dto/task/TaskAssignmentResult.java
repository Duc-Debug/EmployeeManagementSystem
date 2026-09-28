package com.hrm.employeemanagement.application.dto.task;

import java.time.LocalDate;
import java.util.List;

public record TaskAssignmentResult(
        Long taskId,
        String taskCode,
        String taskName,
        List<Long> assigneeIds,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate
) {}

