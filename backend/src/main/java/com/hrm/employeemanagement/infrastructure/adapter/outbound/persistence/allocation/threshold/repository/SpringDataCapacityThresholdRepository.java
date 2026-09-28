package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.entity.CapacityThresholdConfigJpaEntity;

@Repository
public interface SpringDataCapacityThresholdRepository extends JpaRepository<CapacityThresholdConfigJpaEntity, Long> {

    Optional<CapacityThresholdConfigJpaEntity> findByScopeKey(String scopeKey);

    Optional<CapacityThresholdConfigJpaEntity> findByScopeTypeAndOrgUnitId(CapacityThresholdScope scopeType, Long orgUnitId);
}
