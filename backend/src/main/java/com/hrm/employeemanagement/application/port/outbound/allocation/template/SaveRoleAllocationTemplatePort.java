package com.hrm.employeemanagement.application.port.outbound.allocation.template;

import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;

public interface SaveRoleAllocationTemplatePort {
    ProjectRoleAllocationTemplate save(ProjectRoleAllocationTemplate template);
}

