package com.hrm.employeemanagement.infrastructure.transaction.projecttemplate;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase;

public class TransactionalCreateProjectFromTemplateUseCase implements CreateProjectFromTemplateUseCase {

    private final CreateProjectFromTemplateUseCase delegate;

    public TransactionalCreateProjectFromTemplateUseCase(CreateProjectFromTemplateUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CreateProjectFromTemplateUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public ProjectResult createProjectFromTemplate(CreateProjectFromTemplateCommand command) {
        return delegate.createProjectFromTemplate(command);
    }
}
