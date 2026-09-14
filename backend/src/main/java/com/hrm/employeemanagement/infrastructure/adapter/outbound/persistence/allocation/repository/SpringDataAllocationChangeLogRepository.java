package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.AllocationChangeLogJpaEntity;

@Repository
public interface SpringDataAllocationChangeLogRepository extends JpaRepository<AllocationChangeLogJpaEntity, Long> {

    List<AllocationChangeLogJpaEntity> findByAllocationIdOrderByChangedAtDesc(Long allocationId);
}
