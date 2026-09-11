package com.hrm.employeemanagement.application.dto.task.cascade;

import java.time.LocalDate;

public record AffectedTaskResult(
        Long taskId,
        String taskCode,
        String taskName,
        LocalDate originalDueDate,
        LocalDate newCalculatedEndDate,
        long delayDays,
        Integer slackDays,
        boolean protectedBySlack,
        String statusDescription
) {
}
