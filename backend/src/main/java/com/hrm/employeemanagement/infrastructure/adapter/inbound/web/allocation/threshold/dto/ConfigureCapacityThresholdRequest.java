package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.threshold.dto;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * DTO Request cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi (NCL-07-CN-004).
 */
public record ConfigureCapacityThresholdRequest(
        CapacityThresholdScope scopeType,
        Long orgUnitId,

        @NotNull(message = "Ngưỡng quá tải không được để trống")
        @DecimalMin(value = "0.0", message = "Ngưỡng quá tải không được nhỏ hơn 0%")
        @DecimalMax(value = "200.0", message = "Ngưỡng quá tải không được vượt quá 200%")
        BigDecimal overloadThreshold,

        @NotNull(message = "Ngưỡng nhàn rỗi không được để trống")
        @DecimalMin(value = "0.0", message = "Ngưỡng nhàn rỗi không được nhỏ hơn 0%")
        @DecimalMax(value = "200.0", message = "Ngưỡng nhàn rỗi không được vượt quá 200%")
        BigDecimal idleThreshold,

        Long version
) {
}
