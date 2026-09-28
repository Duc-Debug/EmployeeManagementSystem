package com.hrm.employeemanagement.application.port.inbound.project;

import java.util.List;
import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;

public interface GetProjectMembersUseCase {
    List<ProjectMemberResult> getProjectMembers(Long projectId);
}
