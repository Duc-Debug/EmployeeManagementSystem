package com.hrm.employeemanagement.infrastructure.transaction.scenario.recruitment;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.AddSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RemoveSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.UpdateSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.AddSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.GetRecruitmentEvaluationUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.GetScenarioSimulatedEmployeesUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RemoveSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RerunRecruitmentScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.UpdateSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.service.scenario.recruitment.RecruitmentScenarioService;

public class TransactionalRecruitmentScenarioUseCaseDecorator implements
        AddSimulatedEmployeeUseCase,
        UpdateSimulatedEmployeeUseCase,
        RemoveSimulatedEmployeeUseCase,
        GetScenarioSimulatedEmployeesUseCase,
        RerunRecruitmentScenarioUseCase,
        GetRecruitmentEvaluationUseCase {

    private final RecruitmentScenarioService delegate;

    public TransactionalRecruitmentScenarioUseCaseDecorator(RecruitmentScenarioService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "RecruitmentScenarioService must not be null");
    }

    @Override
    @Transactional
    public RecruitmentScenarioEvaluationResult addSimulatedEmployee(AddSimulatedEmployeeCommand command) {
        return delegate.addSimulatedEmployee(command);
    }

    @Override
    @Transactional
    public RecruitmentScenarioEvaluationResult updateSimulatedEmployee(UpdateSimulatedEmployeeCommand command) {
        return delegate.updateSimulatedEmployee(command);
    }

    @Override
    @Transactional
    public RecruitmentScenarioEvaluationResult removeSimulatedEmployee(RemoveSimulatedEmployeeCommand command) {
        return delegate.removeSimulatedEmployee(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulatedEmployeeResult> getSimulatedEmployees(Long scenarioId) {
        return delegate.getSimulatedEmployees(scenarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruitmentScenarioEvaluationResult getRecruitmentEvaluation(Long scenarioId) {
        return delegate.getRecruitmentEvaluation(scenarioId);
    }

    @Override
    @Transactional
    public RecruitmentScenarioEvaluationResult rerunRecruitmentScenario(Long scenarioId) {
        return delegate.rerunRecruitmentScenario(scenarioId);
    }
}
