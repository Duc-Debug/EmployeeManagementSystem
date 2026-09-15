package com.hrm.employeemanagement.infrastructure.persistence.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTimesheetEntryRepository extends JpaRepository<TimesheetEntryJpaEntity, Long> {

    List<TimesheetEntryJpaEntity> findByTimesheetId(Long timesheetId);

    List<TimesheetEntryJpaEntity> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(e.hours), 0) FROM TimesheetEntryJpaEntity e " +
           "WHERE e.employeeId = :employeeId AND e.workDate = :workDate " +
           "AND (:excludeId IS NULL OR e.id <> :excludeId)")
    BigDecimal sumHoursByEmployeeIdAndWorkDate(
            @Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate,
            @Param("excludeId") Long excludeId
    );

    @Query("SELECT e FROM TimesheetEntryJpaEntity e JOIN ProjectJpaEntity p ON e.projectId = p.id " +
           "WHERE p.managerId = :managerId AND e.status = :status ORDER BY e.workDate DESC")
    List<TimesheetEntryJpaEntity> findPendingApprovalsByManager(
            @Param("managerId") Long managerId,
            @Param("status") String status
    );
}
