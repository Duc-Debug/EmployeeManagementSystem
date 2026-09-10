package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.project.DeleteProjectResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.EstimateResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectResourceDemandUseCase;
import com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException;

public class RetryableEstimateResourceDemandUseCaseDecorator implements
        EstimateResourceDemandUseCase,
        GetProjectResourceDemandUseCase,
        DeleteProjectResourceDemandUseCase {

    private final TransactionalProjectResourceDemandServiceDecorator transactionalDelegate;
    private final int maxRetries;

    public RetryableEstimateResourceDemandUseCaseDecorator(
            TransactionalProjectResourceDemandServiceDecorator transactionalDelegate) {
        this(transactionalDelegate, 3);
    }

    public RetryableEstimateResourceDemandUseCaseDecorator(
            TransactionalProjectResourceDemandServiceDecorator transactionalDelegate,
            int maxRetries) {
        this.transactionalDelegate = Objects.requireNonNull(
                transactionalDelegate, "TransactionalProjectResourceDemandServiceDecorator must not be null");
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        this.maxRetries = maxRetries;
    }

    @Override
    public ProjectResourceDemandSummaryResult estimateDemand(EstimateResourceDemandCommand command) {
        DuplicateResourceDemandException lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return transactionalDelegate.estimateDemand(command);
            } catch (DuplicateResourceDemandException ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException
                : new DuplicateResourceDemandException("Xung đột dữ liệu nhu cầu nhân sự sau nhiều lần thử lại");
    }

    @Override
    public ProjectResourceDemandSummaryResult getProjectResourceDemands(Long projectId) {
        return transactionalDelegate.getProjectResourceDemands(projectId);
    }

    @Override
    public ProjectResourceDemandSummaryResult deleteDemand(Long projectId, Long roleId) {
        return transactionalDelegate.deleteDemand(projectId, roleId);
    }
}