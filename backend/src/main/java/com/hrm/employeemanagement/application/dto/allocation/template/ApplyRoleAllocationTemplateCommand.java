package com.hrm.employeemanagement.application.dto.allocation.template;

import java.math.BigDecimal;
import java.util.List;

public record ApplyRoleAllocationTemplateCommand(
        Long templateId,
        Long targetProjectId,
        List<RoleAssignmentItemCommand> assignments
) {
    public record RoleAssignmentItemCommand(
            Long roleId,
            Long employeeId,
            BigDecimal hoursPerWeek
    ) {}
}

