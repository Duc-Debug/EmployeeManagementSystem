package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.CreateScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;

public interface CreateSimulationScenarioUseCase {
    ScenarioResult createScenario(CreateScenarioCommand command);
}
