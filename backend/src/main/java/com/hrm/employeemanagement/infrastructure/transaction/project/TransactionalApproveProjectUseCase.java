package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.ApproveProjectUseCase;

public class TransactionalApproveProjectUseCase implements ApproveProjectUseCase {

    private final ApproveProjectUseCase delegate;

    public TransactionalApproveProjectUseCase(ApproveProjectUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ApproveProjectUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public ProjectResult approveProject(Long projectId) {
        return delegate.approveProject(projectId);
    }
}
