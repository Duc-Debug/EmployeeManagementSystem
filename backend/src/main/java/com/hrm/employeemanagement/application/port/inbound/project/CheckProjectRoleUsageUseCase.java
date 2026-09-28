package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleUsageResult;

public interface CheckProjectRoleUsageUseCase {
    ProjectRoleUsageResult checkUsage(Long roleId);
}
