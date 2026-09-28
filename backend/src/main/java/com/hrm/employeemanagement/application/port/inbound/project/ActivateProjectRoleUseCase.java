package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;

public interface ActivateProjectRoleUseCase {
    ProjectRoleResult activateProjectRole(Long roleId);
}
