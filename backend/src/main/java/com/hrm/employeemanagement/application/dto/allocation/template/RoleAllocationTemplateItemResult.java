package com.hrm.employeemanagement.application.dto.allocation.template;

import java.math.BigDecimal;

public record RoleAllocationTemplateItemResult(
        Long id,
        Long roleId,
        String roleCode,
        String roleName,
        BigDecimal hoursPerWeek
) {}

