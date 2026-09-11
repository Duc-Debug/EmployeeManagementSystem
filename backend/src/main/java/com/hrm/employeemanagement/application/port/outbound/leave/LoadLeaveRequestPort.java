package com.hrm.employeemanagement.application.port.outbound.leave;

import com.hrm.employeemanagement.domain.leave.LeaveRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoadLeaveRequestPort {
    Optional<LeaveRequest> findById(Long id);

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    /**
     * TC-02: Kiểm tra xem nhân viên đã có đơn nghỉ nào (khác REJECTED, CANCELLED)
     * bị trùng với khoảng [startDate, endDate] hay chưa.
     */
    boolean existsOverlappingLeave(Long employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * NCL-05-CN-003: Lấy danh sách các đơn xin nghỉ phép đang chờ duyệt (PENDING).
     */
    List<LeaveRequest> findPendingRequests();
}
