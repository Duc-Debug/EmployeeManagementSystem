package com.hrm.employeemanagement.application.dto.allocation.threshold;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Result DTO trả về thông tin cấu hình ngưỡng cảnh báo năng lực.
 * isDefault: true nếu đang dùng giá trị mặc định của hệ thống (100% / 50%).
 * isInherited: true nếu cấu hình của ORG_UNIT được kế thừa từ COMPANY (do ORG_UNIT chưa có cấu hình riêng).
 */
public record CapacityThresholdResult(
        Long id,
        CapacityThresholdScope scopeType,
        Long orgUnitId,
        BigDecimal overloadThreshold,
        BigDecimal idleThreshold,
        boolean isDefault,
        boolean isInherited,
        Long version,
        LocalDateTime updatedAt,
        Long updatedBy,
        String updaterName
) {
}
