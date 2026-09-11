package com.hrm.employeemanagement.application.port.inbound.project;

import java.util.List;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;

public interface GetProjectRolesUseCase {

    List<ProjectRoleResult> getProjectRoles();

    default List<ProjectRoleResult> getProjectRoles(boolean includeInactive) {
        return getProjectRoles();
    }
}
