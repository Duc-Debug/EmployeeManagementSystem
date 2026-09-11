package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataLeaveRequestRepository extends JpaRepository<LeaveRequestJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LeaveRequestJpaEntity l WHERE l.id = :id")
    Optional<LeaveRequestJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT l FROM LeaveRequestJpaEntity l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.status = 'APPROVED' " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<LeaveRequestJpaEntity> findApprovedLeavesBetween(@Param("employeeId") Long employeeId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Deprecated
    @Query("SELECT COALESCE(SUM(l.hoursDeducted), 0.00) FROM LeaveRequestJpaEntity l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.status = 'APPROVED' " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    BigDecimal sumApprovedLeaveHoursBetween(@Param("employeeId") Long employeeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * TC-02: Kiểm tra trùng lặp đơn nghỉ phép (chưa bị từ chối hoặc hủy).
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LeaveRequestJpaEntity l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.status NOT IN ('REJECTED', 'CANCELLED') " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    boolean existsOverlappingLeave(@Param("employeeId") Long employeeId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    List<LeaveRequestJpaEntity> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    /**
     * NCL-05-CN-005: Lấy danh sách đơn nghỉ phép năm (ANNUAL) ở trạng thái PENDING hoặc APPROVED có khoảng ngày giao thoa với năm chỉ định.
     */
    @Query("SELECT l FROM LeaveRequestJpaEntity l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.leaveType = 'ANNUAL' " +
           "AND l.status IN ('APPROVED', 'PENDING') " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate " +
           "ORDER BY l.startDate DESC")
    List<LeaveRequestJpaEntity> findAnnualLeavesInYear(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<LeaveRequestJpaEntity> findByStatusOrderByCreatedAtAsc(String status);

    @Query(value = """
        SELECT lr.*
        FROM leave_requests lr
        WHERE lr.status = 'PENDING'
        ORDER BY lr.created_at ASC, lr.id ASC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<LeaveRequestJpaEntity> findPendingCompanyScope(
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM leave_requests lr
        WHERE lr.status = 'PENDING'
        """, nativeQuery = true)
    long countPendingCompanyScope();

    @Query(value = """
        SELECT lr.*
        FROM leave_requests lr
        JOIN employees e ON e.id = lr.employee_id
        JOIN org_units ou ON ou.id = e.org_unit_id
        JOIN org_units scope ON scope.id = :scopeOrgUnitId
        WHERE lr.status = 'PENDING'
          AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        ORDER BY lr.created_at ASC, lr.id ASC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<LeaveRequestJpaEntity> findPendingBranchScope(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM leave_requests lr
        JOIN employees e ON e.id = lr.employee_id
        JOIN org_units ou ON ou.id = e.org_unit_id
        JOIN org_units scope ON scope.id = :scopeOrgUnitId
        WHERE lr.status = 'PENDING'
          AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        """, nativeQuery = true)
    long countPendingBranchScope(@Param("scopeOrgUnitId") Long scopeOrgUnitId);

    @Query(value = """
        SELECT lr.*
        FROM leave_requests lr
        JOIN employees e ON e.id = lr.employee_id
        WHERE lr.status = 'PENDING'
          AND e.user_id = :currentUserId
        ORDER BY lr.created_at ASC, lr.id ASC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<LeaveRequestJpaEntity> findPendingSelfScope(
            @Param("currentUserId") Long currentUserId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM leave_requests lr
        JOIN employees e ON e.id = lr.employee_id
        WHERE lr.status = 'PENDING'
          AND e.user_id = :currentUserId
        """, nativeQuery = true)
    long countPendingSelfScope(@Param("currentUserId") Long currentUserId);
}
