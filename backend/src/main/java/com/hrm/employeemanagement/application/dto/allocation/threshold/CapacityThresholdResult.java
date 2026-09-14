package com.hrm.employeemanagement.application.dto.allocation.threshold;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Result DTO trả về thông tin cấu hình ngưỡng cảnh báo năng lực.
 */
public record CapacityThresholdResult(
        Long id,
        CapacityThresholdScope scopeType,
        Long orgUnitId,
        BigDecimal overloadThreshold,
        BigDecimal idleThreshold,
        boolean isDefault,
        Long version,
        LocalDateTime updatedAt,
        Long updatedBy,
        String updatedByName
) {
}
