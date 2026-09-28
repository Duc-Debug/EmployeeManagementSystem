package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
                @NotBlank(message = "Tên dự án không được để trống") @Size(max = 255, message = "Tên dự án không được vượt quá 255 ký tự") String projectName,

                Long managerId,

                LocalDate startDate,

                LocalDate endDate,

                @DecimalMin(value = "0.0", inclusive = true, message = "Tổng giờ dự kiến không được nhỏ hơn 0") @Digits(integer = 8, fraction = 2, message = "Tổng giờ dự kiến chỉ được có tối đa 8 chữ số phần nguyên và 2 chữ số thập phân") BigDecimal estimatedHours,

                @Size(max = 2000, message = "Mô tả dự án không được vượt quá 2000 ký tự") String description) {
}