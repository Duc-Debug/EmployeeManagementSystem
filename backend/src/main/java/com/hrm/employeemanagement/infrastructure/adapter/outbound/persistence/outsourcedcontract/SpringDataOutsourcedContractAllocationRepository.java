package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.outsourcedcontract;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;

@Repository
public interface SpringDataOutsourcedContractAllocationRepository extends JpaRepository<WeeklyProjectAllocationJpaEntity, Long> {

    List<WeeklyProjectAllocationJpaEntity> findByEmployeeId(Long employeeId);

    List<WeeklyProjectAllocationJpaEntity> findByEmployeeIdIn(List<Long> employeeIds);
}
