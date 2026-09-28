package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;

public record MilestoneResponse(
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
    public static MilestoneResponse from(MilestoneResult result) {
        if (result == null) {
            return null;
        }
        return new MilestoneResponse(
                result.id(),
                result.projectId(),
                result.name(),
                result.description(),
                result.plannedDate(),
                result.actualDate(),
                result.status(),
                result.delayDays(),
                result.totalLinkedTasks(),
                result.completedLinkedTasks(),
                result.linkedTaskIds(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
