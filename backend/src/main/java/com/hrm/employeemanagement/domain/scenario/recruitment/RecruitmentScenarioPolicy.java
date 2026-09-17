package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecruitmentScenarioPolicy {

    private static final BigDecimal DEFAULT_FULLTIME_WEEKLY_HOURS = new BigDecimal("40.00");
    private static final int DEFAULT_WEEKS_COUNT = 4;

    public static RecruitmentScenarioEvaluation evaluate(
            Long scenarioId,
            List<RoleShortfallDemand> roleDemands,
            List<ScenarioSimulatedEmployee> simulatedEmployees
    ) {
        List<RoleShortfallDemand> effectiveDemands = roleDemands != null ? roleDemands : Collections.emptyList();
        List<ScenarioSimulatedEmployee> effectiveCandidates = simulatedEmployees != null ? simulatedEmployees : Collections.emptyList();

        Map<Long, List<ScenarioSimulatedEmployee>> candidatesByRole = effectiveCandidates.stream()
                .collect(Collectors.groupingBy(ScenarioSimulatedEmployee::getProjectRoleId));

        List<RoleRecruitmentEvaluationResult> roleEvaluations = new ArrayList<>();
        BigDecimal totalOriginalShortfall = BigDecimal.ZERO;
        BigDecimal totalSimulatedCapacity = BigDecimal.ZERO;
        BigDecimal totalRemainingShortfall = BigDecimal.ZERO;
        int overloadedRoleCount = 0;

        for (RoleShortfallDemand demand : effectiveDemands) {
            BigDecimal origHours = demand.shortfallHours() != null ? demand.shortfallHours() : BigDecimal.ZERO;
            List<ScenarioSimulatedEmployee> roleCandidates = candidatesByRole.getOrDefault(demand.roleId(), Collections.emptyList());

            BigDecimal simulatedHours = roleCandidates.stream()
                    .map(ScenarioSimulatedEmployee::calculateSimulatedCapacityHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal remainingHours = origHours.subtract(simulatedHours);
            if (remainingHours.compareTo(BigDecimal.ZERO) < 0) {
                remainingHours = BigDecimal.ZERO;
            }

            int suggestedRecruits;
            if (remainingHours.compareTo(BigDecimal.ZERO) == 0) {
                suggestedRecruits = 0;
            } else {
                BigDecimal standardCapacityPerPerson = DEFAULT_FULLTIME_WEEKLY_HOURS.multiply(BigDecimal.valueOf(DEFAULT_WEEKS_COUNT));
                suggestedRecruits = remainingHours.divide(standardCapacityPerPerson, 0, RoundingMode.CEILING).intValue();
                if (suggestedRecruits == 0) {
                    suggestedRecruits = 1;
                }
            }

            if (remainingHours.compareTo(BigDecimal.ZERO) > 0) {
                overloadedRoleCount++;
            }

            totalOriginalShortfall = totalOriginalShortfall.add(origHours);
            totalSimulatedCapacity = totalSimulatedCapacity.add(simulatedHours);
            totalRemainingShortfall = totalRemainingShortfall.add(remainingHours);

            roleEvaluations.add(new RoleRecruitmentEvaluationResult(
                    demand.roleId(),
                    demand.roleCode(),
                    demand.roleName(),
                    origHours,
                    simulatedHours,
                    remainingHours,
                    roleCandidates.size(),
                    suggestedRecruits
            ));
        }

        boolean isPlanBroken = totalRemainingShortfall.compareTo(BigDecimal.ZERO) > 0;

        return new RecruitmentScenarioEvaluation(
                scenarioId,
                totalOriginalShortfall,
                totalSimulatedCapacity,
                totalRemainingShortfall,
                isPlanBroken,
                overloadedRoleCount,
                roleEvaluations
        );
    }
}
