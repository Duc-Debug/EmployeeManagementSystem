package com.hrm.employeemanagement.application.port.inbound.scenario.recruitment;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.UpdateSimulatedEmployeeCommand;

public interface UpdateSimulatedEmployeeUseCase {
    RecruitmentScenarioEvaluationResult updateSimulatedEmployee(UpdateSimulatedEmployeeCommand command);
}