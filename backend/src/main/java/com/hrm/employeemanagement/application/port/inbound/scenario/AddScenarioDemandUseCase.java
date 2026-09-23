package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.AddScenarioDemandCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioDemandResult;

public interface AddScenarioDemandUseCase {
    ScenarioDemandResult addDemand(AddScenarioDemandCommand command);
}
