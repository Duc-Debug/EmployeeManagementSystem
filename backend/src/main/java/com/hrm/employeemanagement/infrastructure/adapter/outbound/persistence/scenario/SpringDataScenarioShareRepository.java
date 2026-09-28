package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioShareJpaEntity;

@Repository
public interface SpringDataScenarioShareRepository extends JpaRepository<ScenarioShareJpaEntity, Long> {

    @Query("SELECT s FROM ScenarioShareJpaEntity s WHERE s.scenarioId = :scenarioId AND s.sharedWithUserId = :userId AND s.revokedAt IS NULL")
    Optional<ScenarioShareJpaEntity> findActiveShare(@Param("scenarioId") Long scenarioId, @Param("userId") Long userId);

    @Query("SELECT s FROM ScenarioShareJpaEntity s WHERE s.scenarioId = :scenarioId AND s.revokedAt IS NULL ORDER BY s.createdAt DESC")
    List<ScenarioShareJpaEntity> findActiveSharesByScenarioId(@Param("scenarioId") Long scenarioId);

    @Query("SELECT s FROM ScenarioShareJpaEntity s WHERE s.sharedWithUserId = :userId AND s.revokedAt IS NULL ORDER BY s.createdAt DESC")
    List<ScenarioShareJpaEntity> findActiveSharesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) > 0 FROM ScenarioShareJpaEntity s WHERE s.scenarioId = :scenarioId AND s.sharedWithUserId = :userId AND s.revokedAt IS NULL")
    boolean hasActiveShare(@Param("scenarioId") Long scenarioId, @Param("userId") Long userId);
}
