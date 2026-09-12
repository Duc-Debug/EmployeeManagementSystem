package com.hrm.employeemanagement.application.dto.report;

import java.math.BigDecimal;
import java.util.Map;

public record RecruitmentDemandMetrics(
        Map<Long, BigDecimal> demandHoursBySkill,
        BigDecimal unmappedDemandHours,
        int unmappedRoleCount
) {
}
