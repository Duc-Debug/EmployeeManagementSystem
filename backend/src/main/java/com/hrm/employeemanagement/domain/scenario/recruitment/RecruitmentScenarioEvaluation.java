package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;
import java.util.List;

public record RecruitmentScenarioEvaluation(
        Long scenarioId,
        BigDecimal totalOriginalShortfallHours,
        BigDecimal totalSimulatedCapacityHours,
        BigDecimal totalRemainingShortfallHours,
        boolean isPlanBroken,
        int overloadedRoleCount,
        List<RoleRecruitmentEvaluationResult> roleEvaluations
) {
}
