package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.CreateProjectRoleCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;

public interface CreateProjectRoleUseCase {
    ProjectRoleResult createProjectRole(CreateProjectRoleCommand command);
}
