package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity.AllocationPlanSnapshotJpaEntity;

public interface SpringDataAllocationPlanSnapshotRepository extends JpaRepository<AllocationPlanSnapshotJpaEntity, Long> {

    List<AllocationPlanSnapshotJpaEntity> findByPeriodIdOrderBySnapshotVersionDesc(Long periodId);

    Optional<AllocationPlanSnapshotJpaEntity> findFirstByPeriodIdOrderBySnapshotVersionDesc(Long periodId);

    int countByPeriodId(Long periodId);
}
