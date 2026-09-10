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
     * TC-02: Kiểm tra trùng lặp tối ưu tải cao với index và LIMIT 1.
     * Trả về ngay khi gặp bản ghi đầu tiên khớp điều kiện mà không cần duyệt toàn bộ bảng.
     */
    @Query(value = "SELECT EXISTS(" +
                   "SELECT 1 FROM leave_requests " +
                   "WHERE employee_id = :employeeId " +
                   "AND status NOT IN ('REJECTED', 'CANCELLED') " +
                   "AND start_date <= :endDate AND end_date >= :startDate " +
                   "LIMIT 1)", nativeQuery = true)
    long existsOverlappingLeaveNative(@Param("employeeId") Long employeeId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    default boolean existsOverlappingLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return existsOverlappingLeaveNative(employeeId, startDate, endDate) == 1L;
    }

    List<LeaveRequestJpaEntity> findByEmployeeIdOrderByStartDateDesc(Long employeeId);
}
