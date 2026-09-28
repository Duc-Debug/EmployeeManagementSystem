package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;

public interface SaveSimulationScenarioUseCase {
    ScenarioResult saveScenario(Long scenarioId);
}
