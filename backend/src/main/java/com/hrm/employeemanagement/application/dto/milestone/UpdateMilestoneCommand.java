package com.hrm.employeemanagement.application.dto.milestone;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;

public record UpdateMilestoneCommand(
    Long projectId,
    Long milestoneId,
    String name,
    String description,
    LocalDate plannedDate,
    LocalDate actualDate,
    MilestoneStatus status,
    List<Long> linkedTaskIds
) {
}
