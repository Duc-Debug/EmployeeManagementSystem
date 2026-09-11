package com.hrm.employeemanagement.application.dto.task.cascade;

import java.time.LocalDate;
import java.util.List;

public record CascadeDelayWarningResult(
        Long rootTaskId,
        String rootTaskCode,
        String rootTaskName,
        LocalDate originalDueDate,
        LocalDate newActualEndDate,
        long slipDays,
        boolean chainOnTimeDueToSlack,
        List<AffectedTaskResult> affectedTasks,
        List<AffectedMilestoneResult> affectedMilestones,
        String summaryMessage
) {
}
