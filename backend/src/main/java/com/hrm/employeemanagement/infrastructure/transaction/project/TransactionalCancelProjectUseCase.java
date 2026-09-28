package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.CancelProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CancelProjectUseCase;

public class TransactionalCancelProjectUseCase implements CancelProjectUseCase {

    private final CancelProjectUseCase delegate;

    public TransactionalCancelProjectUseCase(CancelProjectUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CancelProjectUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public ProjectResult cancelProject(CancelProjectCommand command) {
        return delegate.cancelProject(command);
    }
}
