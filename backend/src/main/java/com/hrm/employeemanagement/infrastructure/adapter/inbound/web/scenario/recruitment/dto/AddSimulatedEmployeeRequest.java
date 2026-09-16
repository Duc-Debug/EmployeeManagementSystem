package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddSimulatedEmployeeRequest(
        @NotBlank(message = "Tên nhân sự giả định không được để trống")
        @Size(max = 150, message = "Tên nhân sự giả định không được quá 150 ký tự")
        String candidateName,

        @NotNull(message = "Vai trò dự án không được để trống")
        Long projectRoleId,

        Long primarySkillId,

        @DecimalMin(value = "0.01", message = "Giờ chuẩn mỗi tuần phải lớn hơn 0")
        @DecimalMax(value = "80.00", message = "Giờ chuẩn mỗi tuần không được vượt quá 80")
        BigDecimal standardHoursPerWeek,

        @Min(value = 1, message = "Số tuần phải lớn hơn hoặc bằng 1")
        @Max(value = 52, message = "Số tuần không được vượt quá 52")
        Integer weeksCount,

        @Size(max = 1000, message = "Ghi chú không được quá 1000 ký tự")
        String notes
) {
}
