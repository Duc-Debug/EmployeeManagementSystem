package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.RemoveProjectMemberCommand;
import com.hrm.employeemanagement.application.port.inbound.project.RemoveProjectMemberUseCase;

public class TransactionalRemoveProjectMemberUseCase implements RemoveProjectMemberUseCase {

    private final RemoveProjectMemberUseCase delegate;

    public TransactionalRemoveProjectMemberUseCase(RemoveProjectMemberUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "RemoveProjectMemberUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public void removeProjectMember(RemoveProjectMemberCommand command) {
        delegate.removeProjectMember(command);
    }
}
