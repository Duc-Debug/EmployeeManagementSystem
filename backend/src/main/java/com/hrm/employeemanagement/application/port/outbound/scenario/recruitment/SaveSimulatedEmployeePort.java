package com.hrm.employeemanagement.application.port.outbound.scenario.recruitment;

import com.hrm.employeemanagement.domain.scenario.recruitment.ScenarioSimulatedEmployee;

public interface SaveSimulatedEmployeePort {
    ScenarioSimulatedEmployee save(ScenarioSimulatedEmployee employee);
}
