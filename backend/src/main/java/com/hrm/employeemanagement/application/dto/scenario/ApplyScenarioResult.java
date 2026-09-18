package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplyScenarioResult(
        Long scenarioId,
        String scenarioCode,
        Long targetProjectId,
        String targetProjectName,
        String status,
        int appliedAllocationsCount,
        int affectedEmployeesCount,
        LocalDateTime appliedAt,
        String message,
        BigDecimal totalRequestedHours,
        BigDecimal totalAppliedHours,
        BigDecimal totalUnfulfilledHours,
        boolean isPartiallyFulfilled
) {
    public ApplyScenarioResult(
            Long scenarioId,
            String scenarioCode,
            Long targetProjectId,
            String targetProjectName,
            String status,
            int appliedAllocationsCount,
            int affectedEmployeesCount,
            LocalDateTime appliedAt,
            String message
    ) {
        this(scenarioId, scenarioCode, targetProjectId, targetProjectName, status,
                appliedAllocationsCount, affectedEmployeesCount, appliedAt, message,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
    }
}
