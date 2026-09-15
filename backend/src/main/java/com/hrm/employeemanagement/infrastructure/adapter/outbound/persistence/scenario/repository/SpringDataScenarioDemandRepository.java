package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioDemandJpaEntity;

@Repository
public interface SpringDataScenarioDemandRepository extends JpaRepository<ScenarioDemandJpaEntity, Long> {
    List<ScenarioDemandJpaEntity> findByScenarioIdOrderByStartYearAscStartWeekAsc(Long scenarioId);
    void deleteByScenarioId(Long scenarioId);
}
