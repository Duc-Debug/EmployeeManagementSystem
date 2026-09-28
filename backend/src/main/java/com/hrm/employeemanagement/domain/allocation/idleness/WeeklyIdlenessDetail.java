package com.hrm.employeemanagement.domain.allocation.idleness;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain Record biểu diễn chi tiết mức độ tải / nhàn rỗi của một nhân sự trong một tuần cụ thể.
 * Thuộc phạm vi NCL-07-CN-006 (Cảnh báo nhân sự nhàn rỗi kéo dài - QTN-23).
 */
public record WeeklyIdlenessDetail(
        int year,
        int weekNumber,
        BigDecimal standardHours,
        BigDecimal holidayHours,
        BigDecimal approvedLeaveHours,
        BigDecimal availableHours,
        BigDecimal allocatedHours,
        BigDecimal emptyHours,
        BigDecimal utilizationPercentage,
        boolean isUnderutilized,
        boolean isFullLeaveWeek
) {
    public WeeklyIdlenessDetail {
        Objects.requireNonNull(availableHours, "availableHours must not be null");
        Objects.requireNonNull(allocatedHours, "allocatedHours must not be null");
        Objects.requireNonNull(emptyHours, "emptyHours must not be null");
    }
}
