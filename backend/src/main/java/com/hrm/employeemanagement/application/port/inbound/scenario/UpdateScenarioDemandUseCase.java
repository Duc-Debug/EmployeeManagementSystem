package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.UpdateScenarioDemandCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioDemandResult;

public interface UpdateScenarioDemandUseCase {
    ScenarioDemandResult updateDemand(UpdateScenarioDemandCommand command);
}
