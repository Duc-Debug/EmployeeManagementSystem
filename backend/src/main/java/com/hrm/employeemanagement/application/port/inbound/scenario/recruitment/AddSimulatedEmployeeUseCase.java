package com.hrm.employeemanagement.application.port.inbound.scenario.recruitment;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.AddSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;

public interface AddSimulatedEmployeeUseCase {
    RecruitmentScenarioEvaluationResult addSimulatedEmployee(AddSimulatedEmployeeCommand command);
}
