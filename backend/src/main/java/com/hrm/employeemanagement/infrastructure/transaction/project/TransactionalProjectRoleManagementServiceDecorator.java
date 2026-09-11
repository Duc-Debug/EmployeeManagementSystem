package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.project.demand.CreateProjectRoleCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleUsageResult;
import com.hrm.employeemanagement.application.dto.project.demand.UpdateProjectRoleCommand;
import com.hrm.employeemanagement.application.port.inbound.project.ActivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CheckProjectRoleUsageUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.DeactivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectRoleUseCase;
import com.hrm.employeemanagement.application.service.project.ProjectRoleManagementService;

public class TransactionalProjectRoleManagementServiceDecorator implements
        CreateProjectRoleUseCase,
        UpdateProjectRoleUseCase,
        DeactivateProjectRoleUseCase,
        ActivateProjectRoleUseCase,
        CheckProjectRoleUsageUseCase,
        GetProjectRolesUseCase {

    private final ProjectRoleManagementService delegate;

    public TransactionalProjectRoleManagementServiceDecorator(ProjectRoleManagementService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ProjectRoleManagementService must not be null");
    }

    @Override
    @Transactional
    public ProjectRoleResult createProjectRole(CreateProjectRoleCommand command) {
        return delegate.createProjectRole(command);
    }

    @Override
    @Transactional
    public ProjectRoleResult updateProjectRole(UpdateProjectRoleCommand command) {
        return delegate.updateProjectRole(command);
    }

    @Override
    @Transactional
    public ProjectRoleResult deactivateProjectRole(Long roleId) {
        return delegate.deactivateProjectRole(roleId);
    }

    @Override
    @Transactional
    public ProjectRoleResult activateProjectRole(Long roleId) {
        return delegate.activateProjectRole(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectRoleUsageResult checkUsage(Long roleId) {
        return delegate.checkUsage(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectRoleResult> getProjectRoles() {
        return delegate.getProjectRoles();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectRoleResult> getProjectRoles(boolean includeInactive) {
        return delegate.getProjectRoles(includeInactive);
    }
}
