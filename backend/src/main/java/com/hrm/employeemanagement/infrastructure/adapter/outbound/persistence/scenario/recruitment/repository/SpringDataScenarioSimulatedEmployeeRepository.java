package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.entity.ScenarioSimulatedEmployeeJpaEntity;

@Repository
public interface SpringDataScenarioSimulatedEmployeeRepository extends JpaRepository<ScenarioSimulatedEmployeeJpaEntity, Long> {
    List<ScenarioSimulatedEmployeeJpaEntity> findByScenarioId(Long scenarioId);
    List<ScenarioSimulatedEmployeeJpaEntity> findByScenarioIdAndProjectRoleId(Long scenarioId, Long projectRoleId);
    void deleteByScenarioId(Long scenarioId);
}
