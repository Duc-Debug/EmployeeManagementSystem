package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Tên dự án không được để trống")
        @Size(max = 255, message = "Tên dự án không được vượt quá 255 ký tự")
        String projectName,
        @NotNull(message = "Đơn vị tổ chức phụ trách không được để trống") 
        Long orgUnitId,
        Long managerId,
        LocalDate startDate,
        LocalDate endDate,
        @PositiveOrZero(message = "Tổng giờ dự kiến không được nhỏ hơn 0") 
        BigDecimal estimatedHours,
        String description) {
}
