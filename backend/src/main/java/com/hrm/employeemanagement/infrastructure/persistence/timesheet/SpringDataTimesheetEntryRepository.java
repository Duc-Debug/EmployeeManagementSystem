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

    @Query("SELECT e.employeeId, e.projectId, e.workDate, SUM(e.hours) " +
           "FROM TimesheetEntryJpaEntity e " +
           "WHERE e.employeeId IN :employeeIds " +
           "  AND e.workDate BETWEEN :startDate AND :endDate " +
           "  AND e.status = 'APPROVED' " +
           "  AND (:projectId IS NULL OR e.projectId = :projectId) " +
           "GROUP BY e.employeeId, e.projectId, e.workDate")
    List<Object[]> sumApprovedHoursByEmployeesAndDateRange(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("projectId") Long projectId
    );

    @Query("SELECT COUNT(e) > 0 FROM TimesheetEntryJpaEntity e " +
           "WHERE e.employeeId IN :employeeIds " +
           "  AND e.workDate BETWEEN :startDate AND :endDate " +
           "  AND e.status = 'APPROVED' " +
           "  AND (:projectId IS NULL OR e.projectId = :projectId)")
    boolean existsApprovedEntries(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("projectId") Long projectId
    );

    @Query("SELECT e.employeeId, e.billable, SUM(e.hours) " +
           "FROM TimesheetEntryJpaEntity e " +
           "WHERE e.employeeId IN :employeeIds " +
           "  AND e.workDate BETWEEN :startDate AND :endDate " +
           "  AND e.status = 'APPROVED' " +
           "GROUP BY e.employeeId, e.billable")
    List<Object[]> sumApprovedHoursByEmployeesAndDateRangeGroupedByBillable(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
