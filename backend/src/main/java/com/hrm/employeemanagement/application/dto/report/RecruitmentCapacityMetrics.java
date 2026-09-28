package com.hrm.employeemanagement.application.dto.report;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Skill coverage metrics. Capacity is projected to every approved skill an
 * employee can perform, so values in {@code capacityHoursBySkill} must never
 * be summed as a company-wide capacity total.
 */
public record RecruitmentCapacityMetrics(
        Map<Long, BigDecimal> capacityHoursBySkill,
        BigDecimal unattributedCapacityHours
) {
}
