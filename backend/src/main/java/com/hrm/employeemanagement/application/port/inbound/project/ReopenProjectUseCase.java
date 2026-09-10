package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.ReopenProjectCommand;

public interface ReopenProjectUseCase {
    ProjectResult reopenProject(ReopenProjectCommand command);
}
