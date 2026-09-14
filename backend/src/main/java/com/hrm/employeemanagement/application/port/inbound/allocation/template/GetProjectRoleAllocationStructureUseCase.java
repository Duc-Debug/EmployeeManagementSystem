package com.hrm.employeemanagement.application.port.inbound.allocation.template;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.template.ProjectRoleStructureItem;

public interface GetProjectRoleAllocationStructureUseCase {
    List<ProjectRoleStructureItem> getStructureFromProject(Long projectId);
}

