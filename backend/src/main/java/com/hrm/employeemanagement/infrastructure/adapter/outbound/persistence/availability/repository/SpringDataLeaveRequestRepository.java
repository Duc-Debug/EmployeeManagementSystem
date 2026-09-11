package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpringDataLeaveRequestRepository extends JpaRepository<LeaveRequestJpaEntity, Long> {

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

    @Query("SELECT l FROM LeaveRequestJpaEntity l " +
           "WHERE l.employeeId IN :employeeIds " +
           "AND l.status IN ('APPROVED', 'PENDING') " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate " +
           "ORDER BY l.startDate ASC")
    List<LeaveRequestJpaEntity> findDepartmentLeavesBetween(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
