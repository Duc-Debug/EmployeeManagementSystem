package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;

public record RoleRecruitmentEvaluationResult(
        Long roleId,
        String roleCode,
        String roleName,
        BigDecimal originalShortfallHours,
        BigDecimal simulatedCapacityHours,
        BigDecimal remainingShortfallHours,
        int simulatedEmployeesCount,
        int suggestedRecruitsNeeded
) {
}
