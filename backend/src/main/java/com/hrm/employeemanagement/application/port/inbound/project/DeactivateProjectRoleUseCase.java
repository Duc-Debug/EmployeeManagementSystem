package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;

public interface DeactivateProjectRoleUseCase {
    ProjectRoleResult deactivateProjectRole(Long roleId);
}
