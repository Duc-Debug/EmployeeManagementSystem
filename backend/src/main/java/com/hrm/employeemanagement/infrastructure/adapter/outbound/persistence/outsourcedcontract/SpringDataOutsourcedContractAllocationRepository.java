package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.outsourcedcontract;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;

@Repository
public interface SpringDataOutsourcedContractAllocationRepository extends JpaRepository<WeeklyProjectAllocationJpaEntity, Long> {

    List<WeeklyProjectAllocationJpaEntity> findByEmployeeId(Long employeeId);

    List<WeeklyProjectAllocationJpaEntity> findByEmployeeIdIn(List<Long> employeeIds);

    @Query("SELECT a FROM WeeklyProjectAllocationJpaEntity a WHERE a.employeeId IN :employeeIds AND a.year >= :minYear")
    List<WeeklyProjectAllocationJpaEntity> findByEmployeeIdInAndYearGreaterThanEqual(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("minYear") Integer minYear
    );
}
