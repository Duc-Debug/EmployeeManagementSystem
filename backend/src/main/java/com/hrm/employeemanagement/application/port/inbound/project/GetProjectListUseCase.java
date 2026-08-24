package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;

public interface GetProjectListUseCase {
    PageResult<ProjectResult> getProjects(int page, int size);
}
