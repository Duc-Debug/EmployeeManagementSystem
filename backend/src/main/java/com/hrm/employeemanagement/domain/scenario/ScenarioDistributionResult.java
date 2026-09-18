package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Kết quả phân bổ nhu cầu kịch bản xuống nhân sự kèm các chỉ số kế toán (fulfillment metrics).
 */
public record ScenarioDistributionResult(
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap,
        BigDecimal totalRequestedHours,
        BigDecimal totalAppliedHours,
        BigDecimal totalUnfulfilledHours,
        boolean isPartiallyFulfilled
) {
    public ScenarioDistributionResult {
        totalRequestedHours = totalRequestedHours != null ? totalRequestedHours : BigDecimal.ZERO;
        totalAppliedHours = totalAppliedHours != null ? totalAppliedHours : BigDecimal.ZERO;
        totalUnfulfilledHours = totalUnfulfilledHours != null ? totalUnfulfilledHours : BigDecimal.ZERO;
    }
}

