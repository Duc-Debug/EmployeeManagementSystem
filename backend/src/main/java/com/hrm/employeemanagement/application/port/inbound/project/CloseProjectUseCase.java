package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.CloseProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;

public interface CloseProjectUseCase {
    ProjectResult closeProject(CloseProjectCommand command);
}
