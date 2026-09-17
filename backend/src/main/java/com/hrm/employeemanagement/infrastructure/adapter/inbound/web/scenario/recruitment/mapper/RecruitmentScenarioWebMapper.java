package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.mapper;

import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto.RecruitmentScenarioEvaluationResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment.dto.SimulatedEmployeeResponse;

public class RecruitmentScenarioWebMapper {

    public static SimulatedEmployeeResponse toResponse(SimulatedEmployeeResult result) {
        if (result == null) {
            return null;
        }
        return new SimulatedEmployeeResponse(
                result.id(),
                result.scenarioId(),
                result.candidateName(),
                result.projectRoleId(),
                result.projectRoleCode(),
                result.projectRoleName(),
                result.primarySkillId(),
                result.primarySkillName(),
                result.standardHoursPerWeek(),
                result.weeksCount(),
                result.totalSimulatedCapacityHours(),
                result.notes(),
                result.createdBy(),
                result.createdAt()
        );
    }

    public static RecruitmentScenarioEvaluationResponse toResponse(RecruitmentScenarioEvaluationResult result) {
        if (result == null) {
            return null;
        }
        return new RecruitmentScenarioEvaluationResponse(
                result.scenarioId(),
                result.totalOriginalShortfallHours(),
                result.totalSimulatedCapacityHours(),
                result.totalRemainingShortfallHours(),
                result.isPlanBroken(),
                result.overloadedRoleCount(),
                result.totalSimulatedEmployeesCount(),
                result.totalSuggestedRecruitsNeeded(),
                result.roleEvaluations().stream()
                        .map(r -> new RecruitmentScenarioEvaluationResponse.RoleEvaluationResponse(
                                r.roleId(),
                                r.roleCode(),
                                r.roleName(),
                                r.originalShortfallHours(),
                                r.simulatedCapacityHours(),
                                r.remainingShortfallHours(),
                                r.simulatedEmployeesCount(),
                                r.suggestedRecruitsNeeded()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
