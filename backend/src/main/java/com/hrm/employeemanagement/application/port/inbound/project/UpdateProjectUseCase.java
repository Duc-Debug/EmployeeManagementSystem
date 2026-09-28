package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.UpdateProjectCommand;

public interface UpdateProjectUseCase {
    ProjectResult updateProject(UpdateProjectCommand command);
}