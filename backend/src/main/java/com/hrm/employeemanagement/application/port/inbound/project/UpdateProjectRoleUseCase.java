package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.UpdateProjectRoleCommand;

public interface UpdateProjectRoleUseCase {
    ProjectRoleResult updateProjectRole(UpdateProjectRoleCommand command);
}
