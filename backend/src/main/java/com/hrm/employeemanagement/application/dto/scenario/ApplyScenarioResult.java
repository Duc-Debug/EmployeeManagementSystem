package com.hrm.employeemanagement.application.dto.scenario;

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
        String message
) {}

