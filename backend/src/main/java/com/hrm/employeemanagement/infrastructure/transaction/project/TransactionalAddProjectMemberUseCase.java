package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.AddProjectMemberCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.inbound.project.AddProjectMemberUseCase;

public class TransactionalAddProjectMemberUseCase implements AddProjectMemberUseCase {

    private final AddProjectMemberUseCase delegate;

    public TransactionalAddProjectMemberUseCase(AddProjectMemberUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "AddProjectMemberUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public ProjectMemberResult addProjectMember(AddProjectMemberCommand command) {
        return delegate.addProjectMember(command);
    }
}
