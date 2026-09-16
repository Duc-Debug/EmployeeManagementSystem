package com.hrm.employeemanagement.application.port.inbound.scenario.recruitment;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RemoveSimulatedEmployeeCommand;

public interface RemoveSimulatedEmployeeUseCase {
    RecruitmentScenarioEvaluationResult removeSimulatedEmployee(RemoveSimulatedEmployeeCommand command);
}
