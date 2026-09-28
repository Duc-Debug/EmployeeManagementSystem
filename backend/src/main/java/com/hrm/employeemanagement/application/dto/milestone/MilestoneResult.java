package com.hrm.employeemanagement.application.dto.milestone;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;

public record MilestoneResult(
    Long id,
    Long projectId,
    String name,
    String description,
    LocalDate plannedDate,
    LocalDate actualDate,
    MilestoneStatus status,
    long delayDays,
    int totalLinkedTasks,
    int completedLinkedTasks,
    List<Long> linkedTaskIds,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version
) {
}
