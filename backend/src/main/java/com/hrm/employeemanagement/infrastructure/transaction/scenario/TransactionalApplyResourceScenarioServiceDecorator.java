package com.hrm.employeemanagement.infrastructure.transaction.scenario;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.ApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.PreviewApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.RefreshScenarioBaselineUseCase;
import com.hrm.employeemanagement.application.service.scenario.ApplyResourceScenarioService;

public class TransactionalApplyResourceScenarioServiceDecorator implements
        PreviewApplyScenarioUseCase,
        ApplyScenarioUseCase,
        RefreshScenarioBaselineUseCase {

    private final ApplyResourceScenarioService delegate;

    public TransactionalApplyResourceScenarioServiceDecorator(ApplyResourceScenarioService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ApplyResourceScenarioService must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyScenarioPreviewResult previewApplyScenario(Long scenarioId, Long targetProjectId) {
        return delegate.previewApplyScenario(scenarioId, targetProjectId);
    }

    @Override
    @Transactional
    public ApplyScenarioResult applyScenario(ApplyScenarioCommand command) {
        return delegate.applyScenario(command);
    }

    @Override
    @Transactional
    public ScenarioResult refreshScenarioBaseline(Long scenarioId) {
        return delegate.refreshScenarioBaseline(scenarioId);
    }
}

