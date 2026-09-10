package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.ReopenProjectCommand;
import com.hrm.employeemanagement.application.port.inbound.project.ReopenProjectUseCase;

public class TransactionalReopenProjectUseCase implements ReopenProjectUseCase {

    private final ReopenProjectUseCase delegate;

    public TransactionalReopenProjectUseCase(ReopenProjectUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ReopenProjectUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public ProjectResult reopenProject(ReopenProjectCommand command) {
        return delegate.reopenProject(command);
    }
}
