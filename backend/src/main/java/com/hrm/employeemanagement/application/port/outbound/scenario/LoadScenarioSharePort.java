package com.hrm.employeemanagement.application.port.outbound.scenario;

import java.util.List;
import java.util.Optional;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;

public interface LoadScenarioSharePort {
    Optional<ScenarioShare> findActiveShare(Long scenarioId, Long userId);
    List<ScenarioShare> findActiveSharesByScenarioId(Long scenarioId);
    List<ScenarioShare> findActiveSharesByUserId(Long userId);
    boolean hasActiveShare(Long scenarioId, Long userId);
}
