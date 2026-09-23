package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.mapper;

import com.hrm.employeemanagement.domain.scenario.recruitment.ScenarioSimulatedEmployee;
import com.hrm.employeemanagement.domain.scenario.recruitment.SimulatedEmployeeId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.entity.ScenarioSimulatedEmployeeJpaEntity;

public class ScenarioSimulatedEmployeePersistenceMapper {

    public static ScenarioSimulatedEmployee toDomain(ScenarioSimulatedEmployeeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ScenarioSimulatedEmployee(
                entity.getId() != null ? new SimulatedEmployeeId(entity.getId()) : null,
                entity.getScenarioId(),
                entity.getCandidateName(),
                entity.getProjectRoleId(),
                entity.getPrimarySkillId(),
                entity.getStandardHoursPerWeek(),
                entity.getWeeksCount(),
                entity.getNotes(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public static ScenarioSimulatedEmployeeJpaEntity toEntity(ScenarioSimulatedEmployee domain) {
        if (domain == null) {
            return null;
        }
        return new ScenarioSimulatedEmployeeJpaEntity(
                domain.getIdValue(),
                domain.getScenarioId(),
                domain.getCandidateName(),
                domain.getProjectRoleId(),
                domain.getPrimarySkillId(),
                domain.getStandardHoursPerWeek(),
                domain.getWeeksCount(),
                domain.getNotes(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }
}
