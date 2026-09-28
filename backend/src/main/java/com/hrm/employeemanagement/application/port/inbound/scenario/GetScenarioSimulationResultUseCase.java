package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;

public interface GetScenarioSimulationResultUseCase {
    ScenarioSimulationResult getSimulationResult(Long scenarioId);
}
