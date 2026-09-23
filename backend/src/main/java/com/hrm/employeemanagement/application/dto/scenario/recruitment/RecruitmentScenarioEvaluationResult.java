package com.hrm.employeemanagement.application.dto.scenario.recruitment;

import java.math.BigDecimal;
import java.util.List;

public record RecruitmentScenarioEvaluationResult(
        Long scenarioId,
        BigDecimal totalOriginalShortfallHours,
        BigDecimal totalSimulatedCapacityHours,
        BigDecimal totalRemainingShortfallHours,
        boolean isPlanBroken,
        int overloadedRoleCount,
        int totalSimulatedEmployeesCount,
        int totalSuggestedRecruitsNeeded,
        List<RoleEvaluationItemResult> roleEvaluations
) {
    public record RoleEvaluationItemResult(
            Long roleId,
            String roleCode,
            String roleName,
            BigDecimal originalShortfallHours,
            BigDecimal simulatedCapacityHours,
            BigDecimal remainingShortfallHours,
            int simulatedEmployeesCount,
            int suggestedRecruitsNeeded
    ) {}
}
