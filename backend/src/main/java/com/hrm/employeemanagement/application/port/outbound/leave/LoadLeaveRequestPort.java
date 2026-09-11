package com.hrm.employeemanagement.application.port.outbound.leave;

import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoadLeaveRequestPort {
    Optional<LeaveRequest> findById(Long id);

    Optional<LeaveRequest> findByIdForUpdate(Long id);

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    /**
     * TC-02: Kiểm tra xem nhân viên đã có đơn nghỉ nào (khác REJECTED, CANCELLED)
     * bị trùng với khoảng [startDate, endDate] hay chưa.
     */
    boolean existsOverlappingLeave(Long employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * NCL-05-CN-005: Lấy danh sách đơn nghỉ phép của nhân viên trong một năm dương lịch.
     */
    default List<LeaveRequest> findByEmployeeIdAndYear(Long employeeId, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        return findByEmployeeId(employeeId).stream()
                .filter(r -> r.getStartDate() != null && !r.getStartDate().isBefore(startOfYear) && !r.getStartDate().isAfter(endOfYear))
                .toList();
    }

    /**
     * NCL-05-CN-003: Lấy danh sách các đơn xin nghỉ phép đang chờ duyệt (PENDING).
     */
    List<LeaveRequest> findPendingRequests();

    /**
     * Lấy danh sách đơn nghỉ phép chờ duyệt theo DataScope kết hợp phân trang trực tiếp từ DB.
     */
    PageResult<LeaveRequest> findPendingRequests(
            DataScope dataScope,
            Long scopeOrgUnitId,
            Long currentUserId,
            int page,
            int size
    );
}
