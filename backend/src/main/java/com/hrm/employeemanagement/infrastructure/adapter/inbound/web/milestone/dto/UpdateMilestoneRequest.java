package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone.dto;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.application.dto.milestone.UpdateMilestoneCommand;
import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;

import jakarta.validation.constraints.Size;

public record UpdateMilestoneRequest(
        @Size(max = 255, message = "Tên mốc tiến độ không được vượt quá 255 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
        String description,

        LocalDate plannedDate,
        LocalDate actualDate,
        MilestoneStatus status,
        List<Long> linkedTaskIds
) {
    public UpdateMilestoneCommand toCommand(Long projectId, Long milestoneId) {
        return new UpdateMilestoneCommand(
                projectId,
                milestoneId,
                name,
                description,
                plannedDate,
                actualDate,
                status,
                linkedTaskIds);
    }
}
