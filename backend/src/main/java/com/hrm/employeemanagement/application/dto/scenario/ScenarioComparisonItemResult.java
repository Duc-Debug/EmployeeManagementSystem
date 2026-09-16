package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import java.util.List;

public record ScenarioComparisonItemResult(
        Long scenarioId,
        String scenarioCode,
        String scenarioName,
        String description,
        Long orgUnitId,
        String orgUnitName,
        String status,
        int fromYear,
        int fromWeek,
        int durationWeeks,
        int overloadedEmployeesCount,
        BigDecimal totalShortfallHours,
        BigDecimal totalDemandHours,
        BigDecimal totalWorkloadHours,
        BigDecimal totalAvailableHours,
        BigDecimal averageUtilizationPercentage,
        BigDecimal peakUtilizationPercentage,
        List<WeeklySimulationMetricResult> weeklyMetrics,
        List<OverloadedEmployeeSummaryResult> overloadedEmployees
) {
}
