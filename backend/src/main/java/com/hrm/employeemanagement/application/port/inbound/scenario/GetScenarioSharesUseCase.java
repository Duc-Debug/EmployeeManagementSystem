package com.hrm.employeemanagement.application.port.inbound.scenario;

import java.util.List;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioShareResult;

public interface GetScenarioSharesUseCase {
    List<ScenarioShareResult> getActiveShares(Long scenarioId);
}
