package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioAllocationSnapshotJpaEntity;

@Repository
public interface SpringDataScenarioSnapshotRepository extends JpaRepository<ScenarioAllocationSnapshotJpaEntity, Long> {
    List<ScenarioAllocationSnapshotJpaEntity> findByScenarioId(Long scenarioId);
    void deleteByScenarioId(Long scenarioId);
}
