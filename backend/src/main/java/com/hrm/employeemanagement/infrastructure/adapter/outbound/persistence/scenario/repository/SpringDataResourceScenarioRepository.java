package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ResourceScenarioJpaEntity;

@Repository
public interface SpringDataResourceScenarioRepository extends JpaRepository<ResourceScenarioJpaEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ResourceScenarioJpaEntity s WHERE s.id = :id")
    Optional<ResourceScenarioJpaEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<ResourceScenarioJpaEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<ResourceScenarioJpaEntity> findByOrgUnitIdInOrderByCreatedAtDesc(List<Long> orgUnitIds);
    List<ResourceScenarioJpaEntity> findAllByOrderByCreatedAtDesc();
}
