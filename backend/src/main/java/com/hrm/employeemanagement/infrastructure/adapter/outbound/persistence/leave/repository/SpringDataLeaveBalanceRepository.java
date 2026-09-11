package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.entity.EmployeeLeaveBalanceJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface SpringDataLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalanceJpaEntity, Long> {

    Optional<EmployeeLeaveBalanceJpaEntity> findByEmployeeIdAndYearNumber(Long employeeId, int yearNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM EmployeeLeaveBalanceJpaEntity b WHERE b.employeeId = :employeeId AND b.yearNumber = :yearNumber")
    Optional<EmployeeLeaveBalanceJpaEntity> findByEmployeeIdAndYearNumberWithLock(
            @Param("employeeId") Long employeeId,
            @Param("yearNumber") int yearNumber
    );

    @Modifying
    @Query(value = "INSERT INTO employee_leave_balances (employee_id, year_number, entitled_days, carried_over_days, created_at, updated_at) " +
            "SELECT :employeeId, :yearNumber, :entitledDays, :carriedOverDays, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP " +
            "WHERE NOT EXISTS (SELECT 1 FROM employee_leave_balances WHERE employee_id = :employeeId AND year_number = :yearNumber)",
            nativeQuery = true)
    int insertIfNotExists(
            @Param("employeeId") Long employeeId,
            @Param("yearNumber") int yearNumber,
            @Param("entitledDays") BigDecimal entitledDays,
            @Param("carriedOverDays") BigDecimal carriedOverDays
    );
}

