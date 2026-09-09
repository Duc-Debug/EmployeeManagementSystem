package com.hrm.employeemanagement.infrastructure.transaction.milestone;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.milestone.CreateMilestoneCommand;
import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.port.inbound.milestone.CreateMilestoneUseCase;

public class TransactionalCreateMilestoneUseCase implements CreateMilestoneUseCase {

    private final CreateMilestoneUseCase delegate;

    public TransactionalCreateMilestoneUseCase(CreateMilestoneUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CreateMilestoneUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public MilestoneResult createMilestone(CreateMilestoneCommand command) {
        return delegate.createMilestone(command);
    }
}
