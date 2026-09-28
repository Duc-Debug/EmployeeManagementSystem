package com.hrm.employeemanagement.application.port.outbound.scenario.recruitment;

import java.util.List;
import java.util.Optional;
import com.hrm.employeemanagement.domain.scenario.recruitment.ScenarioSimulatedEmployee;
import com.hrm.employeemanagement.domain.scenario.recruitment.SimulatedEmployeeId;

public interface LoadSimulatedEmployeePort {
    Optional<ScenarioSimulatedEmployee> findById(SimulatedEmployeeId id);
    List<ScenarioSimulatedEmployee> findByScenarioId(Long scenarioId);
}
