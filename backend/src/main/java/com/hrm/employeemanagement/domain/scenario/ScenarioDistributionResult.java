package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Kết quả phân bổ nhu cầu kịch bản xuống nhân sự kèm các chỉ số kế toán (fulfillment metrics)
 * và danh sách chi tiết các nhu cầu bị thiếu hụt theo tuần (unfulfilled details).
 */
public record ScenarioDistributionResult(
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap,
        BigDecimal totalRequestedHours,
        BigDecimal totalAppliedHours,
        BigDecimal totalUnfulfilledHours,
        boolean isPartiallyFulfilled,
        List<UnfulfilledDemandDetail> unfulfilledDetails
) {
    public ScenarioDistributionResult {
        totalRequestedHours = totalRequestedHours != null ? totalRequestedHours : BigDecimal.ZERO;
        totalAppliedHours = totalAppliedHours != null ? totalAppliedHours : BigDecimal.ZERO;
        totalUnfulfilledHours = totalUnfulfilledHours != null ? totalUnfulfilledHours : BigDecimal.ZERO;
        unfulfilledDetails = unfulfilledDetails != null ? List.copyOf(unfulfilledDetails) : List.of();
    }

    public ScenarioDistributionResult(
            Map<Long, Map<String, BigDecimal>> empDemandHoursMap,
            BigDecimal totalRequestedHours,
            BigDecimal totalAppliedHours,
            BigDecimal totalUnfulfilledHours,
            boolean isPartiallyFulfilled
    ) {
        this(empDemandHoursMap, totalRequestedHours, totalAppliedHours, totalUnfulfilledHours, isPartiallyFulfilled, List.of());
    }
}
