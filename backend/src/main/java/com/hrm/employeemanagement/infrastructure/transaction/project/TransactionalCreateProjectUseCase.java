package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;

public class TransactionalCreateProjectUseCase implements CreateProjectUseCase{
    private final CreateProjectUseCase delegate;
    public TransactionalCreateProjectUseCase(CreateProjectUseCase delegate){
        this.delegate = Objects.requireNonNull(delegate,"CreateProjectUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public ProjectResult createProject(CreateProjectCommand command){
        return delegate.createProject(command);
    }
}
