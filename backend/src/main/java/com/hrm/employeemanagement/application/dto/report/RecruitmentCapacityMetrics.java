package com.hrm.employeemanagement.application.dto.report;

import java.math.BigDecimal;
import java.util.Map;

public record RecruitmentCapacityMetrics(
        Map<Long, BigDecimal> capacityHoursBySkill,
        BigDecimal unattributedCapacityHours
) {
}
