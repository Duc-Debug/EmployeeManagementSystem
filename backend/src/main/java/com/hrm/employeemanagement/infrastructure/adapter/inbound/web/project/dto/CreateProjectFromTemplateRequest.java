package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectFromTemplateRequest(
        @NotNull(message = "Mã mẫu dự án không được để trống")
        Long templateId,

        @NotBlank(message = "Tên dự án không được để trống")
        @Size(max = 255, message = "Tên dự án không được vượt quá 255 ký tự")
        String projectName,

        @NotNull(message = "Đơn vị tổ chức phụ trách không được để trống")
        Long orgUnitId,

        Long managerId,
        LocalDate startDate,
        LocalDate endDate,

        @Size(max = 2000, message = "Mô tả dự án không được vượt quá 2000 ký tự")
        String description
) {
}
