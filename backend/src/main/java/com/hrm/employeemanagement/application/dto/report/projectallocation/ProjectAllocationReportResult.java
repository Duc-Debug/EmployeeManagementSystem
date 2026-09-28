package com.hrm.employeemanagement.application.dto.report.projectallocation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectAllocationReportResult(
        Long projectId,
        String projectCode,
        String projectName,
        String status,
        Long orgUnitId,
        String orgUnitName,
        Long managerId,
        String managerName,
        String startDate,
        String endDate,
        int fromYear,
        int fromWeek,
        int toYear,
        int toWeek,
        BigDecimal totalEstimatedHours,
        BigDecimal totalDemandHours,
        BigDecimal totalAllocatedHours,
        BigDecimal totalShortfallHours,
        BigDecimal totalSurplusHours,
        BigDecimal fulfillmentRate,
        int shortageWeeksCount,
        List<WeeklyProjectSummaryItem> weeklySummaries,
        List<RoleAllocationBreakdownItem> roleBreakdowns,
        List<ShortageAlertItem> shortageAlerts,
        LocalDateTime generatedAt
) {
}