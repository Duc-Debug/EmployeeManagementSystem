package com.hrm.employeemanagement.application.dto.task.cascade;

import java.time.LocalDate;

public record AffectedMilestoneResult(
        Long milestoneId,
        String milestoneName,
        LocalDate plannedDate,
        LocalDate newCalculatedDate,
        long delayDays,
        boolean protectedBySlack,
        String statusDescription
) {
}
