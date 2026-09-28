package com.hrm.employeemanagement.application.port.inbound.allocation.template;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateSummaryResult;

public interface GetRoleAllocationTemplatesUseCase {
    List<RoleAllocationTemplateSummaryResult> getAllTemplates();
    RoleAllocationTemplateDetailResult getTemplateById(Long id);
}

