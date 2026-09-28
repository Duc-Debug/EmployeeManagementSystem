package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;

public interface GetProjectDetailUseCase {
    ProjectResult getProjectById(Long projectId);
}
