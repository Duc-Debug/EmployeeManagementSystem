package com.hrm.employeemanagement.application.port.inbound.scenario.recruitment;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;

public interface RerunRecruitmentScenarioUseCase {
    RecruitmentScenarioEvaluationResult rerunRecruitmentScenario(Long scenarioId);
}
