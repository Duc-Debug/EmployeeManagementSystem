package com.hrm.employeemanagement.infrastructure.transaction.milestone;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.dto.milestone.UpdateMilestoneCommand;
import com.hrm.employeemanagement.application.port.inbound.milestone.UpdateMilestoneUseCase;

public class TransactionalUpdateMilestoneUseCase implements UpdateMilestoneUseCase {

    private final UpdateMilestoneUseCase delegate;

    public TransactionalUpdateMilestoneUseCase(UpdateMilestoneUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "UpdateMilestoneUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public MilestoneResult updateMilestone(UpdateMilestoneCommand command) {
        return delegate.updateMilestone(command);
    }
}
