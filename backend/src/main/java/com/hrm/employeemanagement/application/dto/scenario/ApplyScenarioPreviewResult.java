package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import java.util.List;

public record ApplyScenarioPreviewResult(
        Long scenarioId,
        String scenarioCode,
        String scenarioName,
        Long targetProjectId,
        String targetProjectName,
        boolean isBaselineStale,
        List<String> staleReasons,
        List<WeeklyHeaderResult> weeks,
        List<EmployeeComparisonRowResult> employeeComparisons,
        int affectedEmployeesCount,
        BigDecimal totalAdditionalHours
) {
    public record WeeklyHeaderResult(
            int year,
            int weekNumber,
            String weekLabel
    ) {}

    public record WeeklyComparisonCellResult(
            int year,
            int weekNumber,
            BigDecimal currentProjectHours,
            BigDecimal currentTotalAllocatedHours,
            BigDecimal scenarioAdditionalHours,
            BigDecimal newProjectHours,
            BigDecimal newTotalAllocatedHours,
            BigDecimal availableHours,
            boolean isOverloaded
    ) {}

    public record EmployeeComparisonRowResult(
            Long employeeId,
            String employeeCode,
            String employeeName,
            String professionalRole,
            List<WeeklyComparisonCellResult> weeklyCells,
            BigDecimal totalScenarioHours
    ) {}
}

