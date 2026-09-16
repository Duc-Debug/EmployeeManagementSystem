package com.hrm.employeemanagement.infrastructure.transaction.scenario;

import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.application.dto.scenario.CreateScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.CreateSimulationScenarioUseCase;
import com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioCodeException;

public class RetryableCreateSimulationScenarioUseCaseDecorator implements CreateSimulationScenarioUseCase {

    private final CreateSimulationScenarioUseCase transactionalDelegate;
    private final int maxRetries;

    public RetryableCreateSimulationScenarioUseCaseDecorator(CreateSimulationScenarioUseCase transactionalDelegate) {
        this(transactionalDelegate, 5);
    }

    public RetryableCreateSimulationScenarioUseCaseDecorator(CreateSimulationScenarioUseCase transactionalDelegate, int maxRetries) {
        this.transactionalDelegate = Objects.requireNonNull(transactionalDelegate, "CreateSimulationScenarioUseCase delegate must not be null");
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        this.maxRetries = maxRetries;
    }

    @Override
    public ScenarioResult createScenario(CreateScenarioCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dữ liệu kịch bản không được để trống");
        }

        boolean isClientProvidedCode = command.code() != null && !command.code().trim().isEmpty();

        // 1. Client-provided code: unique constraint is the source of truth, do NOT retry
        if (isClientProvidedCode) {
            try {
                return transactionalDelegate.createScenario(command);
            } catch (DataIntegrityViolationException ex) {
                throw new DuplicateScenarioCodeException(command.code().trim());
            }
        }

        // 2. Auto-generated code: retry in a fresh transaction if collision occurs
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return transactionalDelegate.createScenario(command);
            } catch (DuplicateScenarioCodeException | DataIntegrityViolationException ex) {
                lastException = ex;
            }
        }

        throw lastException != null ? lastException : new DuplicateScenarioCodeException("Không thể tạo mã kịch bản sau nhiều lần thử");
    }
}
