package com.hrm.employeemanagement.application.port.inbound.scenario;

import java.util.List;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;

public interface ListSimulationScenariosUseCase {
    List<ScenarioResult> listScenarios(Long orgUnitId);
}
