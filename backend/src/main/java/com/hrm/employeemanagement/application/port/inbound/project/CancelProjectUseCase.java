package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.CancelProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;

public interface CancelProjectUseCase {
    ProjectResult cancelProject(CancelProjectCommand command);
}
