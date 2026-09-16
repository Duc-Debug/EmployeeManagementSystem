package com.hrm.employeemanagement.application.dto.allocation.idleness;

import java.math.BigDecimal;

/**
 * DTO chi tiết từng tuần của nhân sự trong khoảng thời gian rà soát.
 */
public record ProlongedIdlenessWeeklyDetailResult(
        int year,
        int weekNumber,
        BigDecimal availableHours,
        BigDecimal allocatedHours,
        BigDecimal emptyHours,
        BigDecimal utilizationPercentage,
        boolean isUnderutilized,
        boolean isFullLeaveWeek
) {
}
