package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DeclareWeeklyAvailabilityRequest(
        @NotNull(message = "Năm không được để trống")
        @Min(value = 2000, message = "Năm phải từ 2000 trở lên")
        @Max(value = 2100, message = "Năm không được vượt quá 2100")
        Integer year,

        @NotNull(message = "Số tuần không được để trống")
        @Min(value = 1, message = "Số tuần phải từ 1 đến 53")
        @Max(value = 53, message = "Số tuần phải từ 1 đến 53")
        Integer weekNumber,

        @NotNull(message = "Số giờ chuẩn không được để trống")
        @Min(value = 1, message = "Số giờ làm việc chuẩn mỗi tuần phải lớn hơn 0")
        @Max(value = 168, message = "Số giờ làm việc chuẩn mỗi tuần không được vượt quá 168 giờ")
        Integer standardHours
) {}
