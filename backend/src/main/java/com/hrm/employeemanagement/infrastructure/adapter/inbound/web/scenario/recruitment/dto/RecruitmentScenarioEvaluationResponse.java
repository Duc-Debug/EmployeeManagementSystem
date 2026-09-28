package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecruitmentScenarioEvaluationResponse(
        Long scenarioId,
        BigDecimal totalOriginalShortfallHours,
        BigDecimal totalSimulatedCapacityHours,
        BigDecimal totalRemainingShortfallHours,
        @JsonProperty("isPlanBroken")
        boolean isPlanBroken,
        int overloadedRoleCount,
        int totalSimulatedEmployeesCount,
        int totalSuggestedRecruitsNeeded,
        List<RoleEvaluationResponse> roleEvaluations
) {
    public record RoleEvaluationResponse(
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
