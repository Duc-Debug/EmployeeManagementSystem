package com.hrm.employeemanagement.application.dto.report;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Skill requirement metrics. A role's hours appear under every skill required
 * by that role, so values in {@code demandHoursBySkill} are non-additive.
 */
public record RecruitmentDemandMetrics(
        Map<Long, BigDecimal> demandHoursBySkill,
        BigDecimal unmappedDemandHours,
        int unmappedRoleCount
) {
}
