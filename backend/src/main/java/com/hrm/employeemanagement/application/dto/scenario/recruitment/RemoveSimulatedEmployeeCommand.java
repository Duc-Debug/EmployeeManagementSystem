package com.hrm.employeemanagement.application.dto.scenario.recruitment;

public record RemoveSimulatedEmployeeCommand(
        Long scenarioId,
        Long employeeId
) {
}
