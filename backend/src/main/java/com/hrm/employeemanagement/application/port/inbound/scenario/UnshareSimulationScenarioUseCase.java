package com.hrm.employeemanagement.application.port.inbound.scenario;

public interface UnshareSimulationScenarioUseCase {
    void unshareScenario(Long scenarioId, Long sharedWithUserId);
}
