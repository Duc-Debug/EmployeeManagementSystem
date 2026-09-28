package com.hrm.employeemanagement.application.dto.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Skill requirement metrics. A role's hours appear under every skill required
 * by that role, so values in {@code demandHoursBySkill} are non-additive.
 */
public record RecruitmentDemandMetrics(
        Map<Long, BigDecimal> demandHoursBySkill,
        BigDecimal unmappedDemandHours,
        int unmappedRoleCount,
        Map<Long, List<String>> projectNamesBySkill
) {
    public RecruitmentDemandMetrics(
            Map<Long, BigDecimal> demandHoursBySkill,
            BigDecimal unmappedDemandHours,
            int unmappedRoleCount
    ) {
        this(demandHoursBySkill, unmappedDemandHours, unmappedRoleCount, Map.of());
    }
}
