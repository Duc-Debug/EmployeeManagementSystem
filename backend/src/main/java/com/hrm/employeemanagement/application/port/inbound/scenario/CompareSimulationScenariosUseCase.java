package com.hrm.employeemanagement.application.port.inbound.scenario;

import com.hrm.employeemanagement.application.dto.scenario.CompareScenariosCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonResult;

public interface CompareSimulationScenariosUseCase {
    ScenarioComparisonResult compareScenarios(CompareScenariosCommand command);
}
