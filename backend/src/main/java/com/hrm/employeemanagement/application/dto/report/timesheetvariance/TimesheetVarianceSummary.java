package com.hrm.employeemanagement.application.dto.report.timesheetvariance;

import java.math.BigDecimal;

/**
 * Tổng hợp số liệu KPI của báo cáo đối chiếu giờ phân bổ vs thực tế.
 */
public record TimesheetVarianceSummary(
        BigDecimal totalAllocatedHours,
        BigDecimal totalActualApprovedHours,
        BigDecimal totalVarianceHours,
        Integer totalEmployees,
        Integer totalWeeks,
        Integer positiveVarianceCount,
        Integer negativeVarianceCount,
        Integer onTrackCount,
        Integer noActualDataCount
) {
}
