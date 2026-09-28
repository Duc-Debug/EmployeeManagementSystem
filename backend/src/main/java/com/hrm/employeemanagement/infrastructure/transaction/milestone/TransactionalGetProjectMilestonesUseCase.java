package com.hrm.employeemanagement.infrastructure.transaction.milestone;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.port.inbound.milestone.GetProjectMilestonesUseCase;

public class TransactionalGetProjectMilestonesUseCase implements GetProjectMilestonesUseCase {

    private final GetProjectMilestonesUseCase delegate;

    public TransactionalGetProjectMilestonesUseCase(GetProjectMilestonesUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetProjectMilestonesUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneResult> getProjectMilestones(Long projectId) {
        return delegate.getProjectMilestones(projectId);
    }
}
