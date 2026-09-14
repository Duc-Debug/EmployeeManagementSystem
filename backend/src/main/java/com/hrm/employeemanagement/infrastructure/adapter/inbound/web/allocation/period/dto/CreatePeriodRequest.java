package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period.dto;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePeriodRequest(
        @NotBlank(message = "Tên kỳ kế hoạch không được để trống")
        @Size(max = 150, message = "Tên kỳ kế hoạch không được vượt quá 150 ký tự")
        String name,

        AllocationPeriodType periodType,

        @NotNull(message = "Năm không được để trống")
        @Min(value = 2000, message = "Năm không hợp lệ")
        @Max(value = 2100, message = "Năm không hợp lệ")
        Integer year,

        @NotNull(message = "Tuần bắt đầu không được để trống")
        @Min(value = 1, message = "Tuần bắt đầu phải từ 1 đến 53")
        @Max(value = 53, message = "Tuần bắt đầu phải từ 1 đến 53")
        Integer startWeek,

        @NotNull(message = "Tuần kết thúc không được để trống")
        @Min(value = 1, message = "Tuần kết thúc phải từ 1 đến 53")
        @Max(value = 53, message = "Tuần kết thúc phải từ 1 đến 53")
        Integer endWeek
) {
}
