package com.hrm.employeemanagement.application.port.outbound.scenario;

import java.util.List;
import java.util.Optional;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;

public interface LoadScenarioDemandPort {
    Optional<ScenarioDemand> findById(Long id);
    List<ScenarioDemand> findByScenarioId(Long scenarioId);
}
