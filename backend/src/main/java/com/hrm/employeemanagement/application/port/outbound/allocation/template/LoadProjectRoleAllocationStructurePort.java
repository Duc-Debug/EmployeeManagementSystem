package com.hrm.employeemanagement.application.port.outbound.allocation.template;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.template.ProjectRoleStructureItem;

public interface LoadProjectRoleAllocationStructurePort {
    List<ProjectRoleStructureItem> extractRoleStructureFromProject(Long projectId);
}

