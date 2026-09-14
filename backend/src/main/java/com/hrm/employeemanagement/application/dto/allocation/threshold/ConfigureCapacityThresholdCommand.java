package com.hrm.employeemanagement.application.dto.allocation.threshold;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Command cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi theo QTN-23.
 */
public record ConfigureCapacityThresholdCommand(
        CapacityThresholdScope scopeType,
        Long orgUnitId,
        BigDecimal overloadThreshold,
        BigDecimal idleThreshold,
        Long version
) {
}
