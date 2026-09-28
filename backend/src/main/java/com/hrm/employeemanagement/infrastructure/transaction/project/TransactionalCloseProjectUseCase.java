package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.CloseProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CloseProjectUseCase;

public class TransactionalCloseProjectUseCase implements CloseProjectUseCase {

    private final CloseProjectUseCase delegate;

    public TransactionalCloseProjectUseCase(CloseProjectUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CloseProjectUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public ProjectResult closeProject(CloseProjectCommand command) {
        return delegate.closeProject(command);
    }
}
