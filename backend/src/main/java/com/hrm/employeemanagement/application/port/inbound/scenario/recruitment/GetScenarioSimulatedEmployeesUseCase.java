package com.hrm.employeemanagement.application.port.inbound.scenario.recruitment;

import java.util.List;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;

public interface GetScenarioSimulatedEmployeesUseCase {
    List<SimulatedEmployeeResult> getSimulatedEmployees(Long scenarioId);
}
