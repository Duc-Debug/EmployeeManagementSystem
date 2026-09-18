package com.hrm.employeemanagement.application.dto.report.projectallocation;

import java.math.BigDecimal;
import java.util.List;

public record RoleAllocationBreakdownItem(
        Long roleId,
        String roleCode,
        String roleName,
        BigDecimal totalDemandHours,
        BigDecimal totalAllocatedHours,
        BigDecimal totalShortfallHours,
        BigDecimal totalSurplusHours,
        BigDecimal fulfillmentRate,
        List<WeeklyRoleAllocationItem> weeklyRoleMetrics
) {
}