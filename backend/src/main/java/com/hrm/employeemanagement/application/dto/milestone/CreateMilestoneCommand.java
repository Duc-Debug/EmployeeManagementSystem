package com.hrm.employeemanagement.application.dto.milestone;

import java.time.LocalDate;
import java.util.List;

public record CreateMilestoneCommand(
    Long projectId,
    String name,
    String description,
    LocalDate plannedDate,
    List<Long> linkedTaskIds
) {
}
