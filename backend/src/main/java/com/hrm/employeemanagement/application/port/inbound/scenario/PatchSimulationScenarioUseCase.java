package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.PatchScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;

public interface PatchSimulationScenarioUseCase {
    ScenarioResult patchScenario(PatchScenarioCommand command);
}
