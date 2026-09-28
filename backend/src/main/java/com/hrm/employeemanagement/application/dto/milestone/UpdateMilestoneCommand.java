package com.hrm.employeemanagement.application.dto.milestone;

import java.time.LocalDate;
import java.util.List;

public record UpdateMilestoneCommand(
    Long projectId,
    Long milestoneId,
    String name,
    String description,
    LocalDate plannedDate,
    LocalDate actualDate,
    List<Long> linkedTaskIds
) {
}
