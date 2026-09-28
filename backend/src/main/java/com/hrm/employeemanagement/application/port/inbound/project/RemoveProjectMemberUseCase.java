package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.RemoveProjectMemberCommand;

public interface RemoveProjectMemberUseCase {
    void removeProjectMember(RemoveProjectMemberCommand command);
}
