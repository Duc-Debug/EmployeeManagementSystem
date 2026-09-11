package com.hrm.employeemanagement.application.port.outbound.project;

import com.hrm.employeemanagement.domain.project.demand.ProjectRole;

public interface SaveProjectRolePort {
    ProjectRole save(ProjectRole projectRole);

    default void syncEmployeeProfessionalRole(String oldRoleName, String newRoleName) {
    }
}
