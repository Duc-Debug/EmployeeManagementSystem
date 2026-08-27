package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;

public interface CreateProjectUseCase {
    ProjectResult createProject(CreateProjectCommand command);
}