package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.project.EstimateResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectResourceDemandUseCase;
import com.hrm.employeemanagement.application.service.project.ProjectResourceDemandService;

public class TransactionalProjectResourceDemandServiceDecorator implements
        EstimateResourceDemandUseCase,
        GetProjectResourceDemandUseCase {

    private final ProjectResourceDemandService delegate;

    public TransactionalProjectResourceDemandServiceDecorator(ProjectResourceDemandService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ProjectResourceDemandService must not be null");
    }

    @Override
    @Transactional
    public ProjectResourceDemandSummaryResult estimateDemand(EstimateResourceDemandCommand command) {
        return delegate.estimateDemand(command);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResourceDemandSummaryResult getProjectResourceDemands(Long projectId) {
        return delegate.getProjectResourceDemands(projectId);
    }
}