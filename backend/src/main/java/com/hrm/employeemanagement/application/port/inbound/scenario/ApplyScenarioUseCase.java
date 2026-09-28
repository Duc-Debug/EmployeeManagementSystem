package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioResult;

public interface ApplyScenarioUseCase {
    ApplyScenarioResult applyScenario(ApplyScenarioCommand command);
}

