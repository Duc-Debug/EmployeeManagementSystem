package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RecruitmentScenarioPolicy Unit Tests")
class RecruitmentScenarioPolicyTest {

    @Test
    @DisplayName("Bù trừ chính xác 160h thiếu hụt vai trò DEV khi thêm 1 nhân sự giả định 40h/tuần x 4 tuần")
    void testEvaluate_ExactMatchShortfall() {
        Long scenarioId = 1L;
        Long devRoleId = 10L;

        RoleShortfallDemand devDemand = new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("160.00"));
        ScenarioSimulatedEmployee devCandidate = ScenarioSimulatedEmployee.create(
                scenarioId, "Nhân sự DEV giả định 1", devRoleId, null, new BigDecimal("40.00"), 4, "Tuyển thêm", 1L
        );

        RecruitmentScenarioEvaluation evaluation = RecruitmentScenarioPolicy.evaluate(
                scenarioId,
                List.of(devDemand),
                List.of(devCandidate)
        );

        assertEquals(0, new BigDecimal("160.00").compareTo(evaluation.totalOriginalShortfallHours()));
        assertEquals(0, new BigDecimal("160.00").compareTo(evaluation.totalSimulatedCapacityHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(evaluation.totalRemainingShortfallHours()));
        assertFalse(evaluation.isPlanBroken());
        assertEquals(0, evaluation.overloadedRoleCount());

        RoleRecruitmentEvaluationResult roleResult = evaluation.roleEvaluations().getFirst();
        assertEquals(0, BigDecimal.ZERO.compareTo(roleResult.remainingShortfallHours()));
        assertEquals(1, roleResult.simulatedEmployeesCount());
        assertEquals(0, roleResult.suggestedRecruitsNeeded());
    }

    @Test
    @DisplayName("Kịch bản còn vỡ kế hoạch khi nhân sự giả định chưa đủ bù đắp số giờ thiếu")
    void testEvaluate_PartialShortfallCovered() {
        Long scenarioId = 1L;
        Long devRoleId = 10L;

        RoleShortfallDemand devDemand = new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("200.00"));
        ScenarioSimulatedEmployee devCandidate = ScenarioSimulatedEmployee.create(
                scenarioId, "Nhân sự DEV giả định 1", devRoleId, null, new BigDecimal("40.00"), 4, "Tuyển thêm", 1L
        );

        RecruitmentScenarioEvaluation evaluation = RecruitmentScenarioPolicy.evaluate(
                scenarioId,
                List.of(devDemand),
                List.of(devCandidate)
        );

        assertEquals(0, new BigDecimal("200.00").compareTo(evaluation.totalOriginalShortfallHours()));
        assertEquals(0, new BigDecimal("160.00").compareTo(evaluation.totalSimulatedCapacityHours()));
        assertEquals(0, new BigDecimal("40.00").compareTo(evaluation.totalRemainingShortfallHours()));
        assertTrue(evaluation.isPlanBroken());
        assertEquals(1, evaluation.overloadedRoleCount());

        RoleRecruitmentEvaluationResult roleResult = evaluation.roleEvaluations().getFirst();
        assertEquals(0, new BigDecimal("40.00").compareTo(roleResult.remainingShortfallHours()));
        assertEquals(1, roleResult.suggestedRecruitsNeeded());
    }

    @Test
    @DisplayName("Nhiều vai trò dự án: bù đắp toàn bộ giúp kịch bản hết vỡ kế hoạch")
    void testEvaluate_MultipleRolesCovered() {
        Long scenarioId = 1L;
        Long devRoleId = 10L;
        Long testerRoleId = 20L;

        RoleShortfallDemand devDemand = new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("160.00"));
        RoleShortfallDemand testerDemand = new RoleShortfallDemand(testerRoleId, "TESTER", "Tester", new BigDecimal("80.00"));

        ScenarioSimulatedEmployee devCandidate = ScenarioSimulatedEmployee.create(
                scenarioId, "Dev 1", devRoleId, null, new BigDecimal("40.00"), 4, null, 1L
        );
        ScenarioSimulatedEmployee testerCandidate = ScenarioSimulatedEmployee.create(
                scenarioId, "Tester 1", testerRoleId, null, new BigDecimal("20.00"), 4, null, 1L
        );

        RecruitmentScenarioEvaluation evaluation = RecruitmentScenarioPolicy.evaluate(
                scenarioId,
                List.of(devDemand, testerDemand),
                List.of(devCandidate, testerCandidate)
        );

        assertFalse(evaluation.isPlanBroken());
        assertEquals(0, BigDecimal.ZERO.compareTo(evaluation.totalRemainingShortfallHours()));
        assertEquals(0, evaluation.overloadedRoleCount());
    }
}