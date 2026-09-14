package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AdjustAllocationRequest(
        @NotNull(message = "Hành động điều chỉnh (action) không được để trống")
        AdjustmentAction action,

        @DecimalMin(value = "0.01", message = "Số giờ phân bổ phải lớn hơn 0")
        @DecimalMax(value = "168.00", message = "Số giờ phân bổ không được vượt quá 168 giờ")
        BigDecimal newHours,

        @DecimalMin(value = "0.01", message = "Tỷ lệ phần trăm phân bổ phải lớn hơn 0%")
        @DecimalMax(value = "100.00", message = "Tỷ lệ phần trăm không được vượt quá 100%")
        BigDecimal allocationPercentage,

        Integer targetYear,
        Integer targetWeek,
        String varianceReason,
        String overloadReason
) {
}
