package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record EstimateResourceDemandRequest(
        @NotNull(message = "Vai trò chuyên môn không được để trống")
        Long roleId,

        @NotNull(message = "Số giờ nhu cầu mỗi tuần không được để trống")
        @DecimalMin(value = "0.01", inclusive = true, message = "Số giờ nhu cầu mỗi tuần phải lớn hơn 0")
        @DecimalMax(value = "168.00", inclusive = true, message = "Số giờ nhu cầu mỗi tuần không được vượt quá 168 giờ")
        @Digits(integer = 3, fraction = 2, message = "Số giờ nhu cầu chỉ được có tối đa 3 chữ số phần nguyên và 2 chữ số phần thập phân")
        BigDecimal hoursPerWeek) {
}