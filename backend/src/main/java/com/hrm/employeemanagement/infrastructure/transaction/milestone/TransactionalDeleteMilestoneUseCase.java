package com.hrm.employeemanagement.infrastructure.transaction.milestone;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.inbound.milestone.DeleteMilestoneUseCase;

public class TransactionalDeleteMilestoneUseCase implements DeleteMilestoneUseCase {

    private final DeleteMilestoneUseCase delegate;

    public TransactionalDeleteMilestoneUseCase(DeleteMilestoneUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "DeleteMilestoneUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public void deleteMilestone(Long projectId, Long milestoneId) {
        delegate.deleteMilestone(projectId, milestoneId);
    }
}
