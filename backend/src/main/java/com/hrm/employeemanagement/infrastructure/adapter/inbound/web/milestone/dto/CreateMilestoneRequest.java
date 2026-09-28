package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone.dto;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.application.dto.milestone.CreateMilestoneCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMilestoneRequest(
        @NotBlank(message = "Tên mốc tiến độ không được để trống")
        @Size(max = 255, message = "Tên mốc tiến độ không được vượt quá 255 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
        String description,

        @NotNull(message = "Ngày kế hoạch không được để trống")
        LocalDate plannedDate,

        List<@NotNull(message = "Mã công việc liên kết không được để trống") @Positive(message = "Mã công việc liên kết phải là số nguyên dương") Long> linkedTaskIds
) {
    public CreateMilestoneCommand toCommand(Long projectId) {
        return new CreateMilestoneCommand(
                projectId,
                name,
                description,
                plannedDate,
                linkedTaskIds);
    }
}
