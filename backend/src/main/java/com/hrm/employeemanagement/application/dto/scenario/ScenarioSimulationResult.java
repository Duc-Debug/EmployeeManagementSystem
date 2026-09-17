package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ScenarioSimulationResult(
        Long scenarioId,
        String scenarioCode,
        String scenarioName,
        Long orgUnitId,
        String orgUnitName,
        String status,
        LocalDateTime baseSnapshotAt,
        List<WeeklySimulationMetricResult> weeklyMetrics,
        List<OverloadedEmployeeResult> overloadedEmployees,
        List<EmployeeSnapshotRowResult> employeeSnapshots,
        BigDecimal overloadThreshold,
        BigDecimal idleThreshold
) {
}
