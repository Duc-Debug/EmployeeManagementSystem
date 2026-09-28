package com.hrm.employeemanagement.application.port.inbound.scenario;

import java.util.List;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioShareResult;
import com.hrm.employeemanagement.application.dto.scenario.ShareScenarioCommand;

public interface ShareSimulationScenarioUseCase {
    List<ScenarioShareResult> shareScenario(ShareScenarioCommand command);
}
