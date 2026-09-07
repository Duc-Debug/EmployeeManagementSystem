package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.UpdateProjectCommand;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectUseCase;

public class TransactionalUpdateProjectUseCase implements UpdateProjectUseCase {

    private final UpdateProjectUseCase delegate;

    public TransactionalUpdateProjectUseCase(UpdateProjectUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "UpdateProjectUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public ProjectResult updateProject(UpdateProjectCommand command) {
        return delegate.updateProject(command);
    }
}
