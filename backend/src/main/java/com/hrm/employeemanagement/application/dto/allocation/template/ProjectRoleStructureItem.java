package com.hrm.employeemanagement.application.dto.allocation.template;

import java.math.BigDecimal;

public record ProjectRoleStructureItem(
        Long roleId,
        String roleCode,
        String roleName,
        BigDecimal hoursPerWeek
) {}

