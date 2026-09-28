package com.hrm.employeemanagement.application.port.outbound.scenario.recruitment;

import java.util.Optional;

public interface LoadSimulationScenarioPort {
    boolean existsById(Long scenarioId);
    Optional<SimulationScenarioInfo> findById(Long scenarioId);

    record SimulationScenarioInfo(Long id, String scenarioCode, String name, String status) {}
}
