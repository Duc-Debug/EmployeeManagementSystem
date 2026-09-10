package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.AddProjectMemberCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;

public interface AddProjectMemberUseCase {
    ProjectMemberResult addProjectMember(AddProjectMemberCommand command);
}
