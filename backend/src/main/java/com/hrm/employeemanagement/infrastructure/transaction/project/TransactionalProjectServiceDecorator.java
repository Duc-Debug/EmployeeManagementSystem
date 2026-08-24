package com.hrm.employeemanagement.infrastructure.transaction.project;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.application.service.project.ProjectService;

public class TransactionalProjectServiceDecorator
        implements GetProjectListUseCase,
        GetProjectDetailUseCase {

    private final ProjectService delegate;

    public TransactionalProjectServiceDecorator(
            ProjectService delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProjectResult> getProjects(
            int page,
            int size
    ) {
        return delegate.getProjects(
                page,
                size
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResult getProjectById(Long projectId) {
        return delegate.getProjectById(
                projectId
        );
    }
}
