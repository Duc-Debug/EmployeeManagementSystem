package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.ScenarioDetailResult;

public interface GetSimulationScenarioUseCase {
    ScenarioDetailResult getScenarioById(Long scenarioId);
}
