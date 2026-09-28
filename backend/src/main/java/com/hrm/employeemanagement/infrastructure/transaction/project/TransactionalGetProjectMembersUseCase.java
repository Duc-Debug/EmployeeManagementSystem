package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectMembersUseCase;

public class TransactionalGetProjectMembersUseCase implements GetProjectMembersUseCase {

    private final GetProjectMembersUseCase delegate;

    public TransactionalGetProjectMembersUseCase(GetProjectMembersUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetProjectMembersUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResult> getProjectMembers(Long projectId) {
        return delegate.getProjectMembers(projectId);
    }
}
