package com.hrm.employeemanagement.application.dto.project.demand;
import java.math.BigDecimal;
import java.util.List;
public record RoleResourceDemandResult(
        Long roleId,
        String roleCode,
        String roleName,
        BigDecimal totalRoleHours,
        List<WeeklyDemandItemResult> weeklyDemands) {
}