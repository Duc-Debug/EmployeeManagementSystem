package com.hrm.employeemanagement.infrastructure.transaction.scenario;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.scenario.CompareScenariosCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.CompareSimulationScenariosUseCase;

public class TransactionalScenarioComparisonUseCaseDecorator implements CompareSimulationScenariosUseCase {

    private final CompareSimulationScenariosUseCase delegate;

    public TransactionalScenarioComparisonUseCaseDecorator(CompareSimulationScenariosUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CompareSimulationScenariosUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public ScenarioComparisonResult compareScenarios(CompareScenariosCommand command) {
        return delegate.compareScenarios(command);
    }
}