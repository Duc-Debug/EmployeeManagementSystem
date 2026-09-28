package com.hrm.employeemanagement.application.port.inbound.allocation.template;

import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateResult;

public interface ApplyRoleAllocationTemplateUseCase {
    ApplyRoleAllocationTemplateResult applyTemplate(ApplyRoleAllocationTemplateCommand command);
}

