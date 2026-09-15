package com.hrm.employeemanagement.application.port.outbound.scenario;

public interface DeleteScenarioDemandPort {
    void deleteById(Long demandId);
    void deleteAllByScenarioId(Long scenarioId);
}
