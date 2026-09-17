package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.entity.SimulationScenarioJpaEntity;

@Repository
public interface SpringDataSimulationScenarioRepository extends JpaRepository<SimulationScenarioJpaEntity, Long> {
    Optional<SimulationScenarioJpaEntity> findByScenarioCode(String scenarioCode);
}
