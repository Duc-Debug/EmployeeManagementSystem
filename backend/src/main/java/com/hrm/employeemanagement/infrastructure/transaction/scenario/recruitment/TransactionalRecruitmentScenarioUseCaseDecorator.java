package com.hrm.employeemanagement.infrastructure.transaction.scenario.recruitment;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.AddSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RemoveSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.AddSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.GetScenarioSimulatedEmployeesUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RemoveSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RerunRecruitmentScenarioUseCase;
import com.hrm.employeemanagement.application.service.scenario.recruitment.RecruitmentScenarioService;

public class TransactionalRecruitmentScenarioUseCaseDecorator implements
        AddSimulatedEmployeeUseCase,
        RemoveSimulatedEmployeeUseCase,
        GetScenarioSimulatedEmployeesUseCase,
        RerunRecruitmentScenarioUseCase {

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
    public RecruitmentScenarioEvaluationResult removeSimulatedEmployee(RemoveSimulatedEmployeeCommand command) {
        return delegate.removeSimulatedEmployee(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulatedEmployeeResult> getSimulatedEmployees(Long scenarioId) {
        return delegate.getSimulatedEmployees(scenarioId);
    }

    @Override
    @Transactional
    public RecruitmentScenarioEvaluationResult rerunRecruitmentScenario(Long scenarioId) {
        return delegate.rerunRecruitmentScenario(scenarioId);
    }
}
