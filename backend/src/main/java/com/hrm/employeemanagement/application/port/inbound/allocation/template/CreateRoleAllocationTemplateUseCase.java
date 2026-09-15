package com.hrm.employeemanagement.application.port.inbound.allocation.template;

import com.hrm.employeemanagement.application.dto.allocation.template.CreateRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateDetailResult;

public interface CreateRoleAllocationTemplateUseCase {
    RoleAllocationTemplateDetailResult createTemplate(CreateRoleAllocationTemplateCommand command);
}

