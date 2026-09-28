package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.outsourcedcontract;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;

@Repository
public interface SpringDataOutsourcedContractEmployeeRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.isOutsourced = true AND e.contractEndDate IS NOT NULL")
    List<EmployeeJpaEntity> findAllOutsourcedWithContract();
}
