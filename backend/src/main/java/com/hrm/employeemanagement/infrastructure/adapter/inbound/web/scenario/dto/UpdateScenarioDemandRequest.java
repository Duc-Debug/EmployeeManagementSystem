package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateScenarioDemandRequest(
        @NotBlank(message = "Tên nhu cầu không được để trống")
        @Size(max = 255, message = "Tên nhu cầu không được vượt quá 255 ký tự")
        String demandName,

        @NotNull(message = "Số lượng người cần không được để trống")
        @Min(value = 1, message = "Số lượng người cần phải lớn hơn 0")
        Integer headcount,

        @NotNull(message = "Năm bắt đầu không được để trống")
        Integer startYear,

        @NotNull(message = "Tuần bắt đầu không được để trống")
        @Min(value = 1, message = "Tuần bắt đầu phải từ 1 đến 53")
        @Max(value = 53, message = "Tuần bắt đầu phải từ 1 đến 53")
        Integer startWeek,

        @NotNull(message = "Năm kết thúc không được để trống")
        Integer endYear,

        @NotNull(message = "Tuần kết thúc không được để trống")
        @Min(value = 1, message = "Tuần kết thúc phải từ 1 đến 53")
        @Max(value = 53, message = "Tuần kết thúc phải từ 1 đến 53")
        Integer endWeek,

        @NotNull(message = "Số giờ/tuần/người không được để trống")
        @DecimalMin(value = "0.0", message = "Số giờ/tuần/người không được âm")
        BigDecimal hoursPerWeekPerPerson,

        String skillRequirement
) {
}
