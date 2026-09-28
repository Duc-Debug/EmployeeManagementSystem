package com.hrm.employeemanagement.application.dto.scenario;

public record PatchScenarioCommand(
        Long scenarioId,
        String name,
        String note
) {
}
