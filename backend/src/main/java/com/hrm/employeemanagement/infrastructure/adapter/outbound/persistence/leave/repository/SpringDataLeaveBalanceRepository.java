package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.entity.EmployeeLeaveBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalanceJpaEntity, Long> {

    Optional<EmployeeLeaveBalanceJpaEntity> findByEmployeeIdAndYearNumber(Long employeeId, int yearNumber);
}
