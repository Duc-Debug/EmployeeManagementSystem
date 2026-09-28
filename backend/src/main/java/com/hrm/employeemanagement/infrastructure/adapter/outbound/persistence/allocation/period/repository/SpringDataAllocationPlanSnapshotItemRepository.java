package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanSnapshotItemJpaEntity;

public interface SpringDataAllocationPlanSnapshotItemRepository extends JpaRepository<AllocationPlanSnapshotItemJpaEntity, Long> {

    List<AllocationPlanSnapshotItemJpaEntity> findBySnapshotId(Long snapshotId);
}
