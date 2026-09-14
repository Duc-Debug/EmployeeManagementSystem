package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;

/**
 * NCL-05-CN-007: Quản lý nguồn lực từ chối yêu cầu hủy đơn nghỉ phép đã duyệt.
 */
public interface RejectCancelLeaveRequestUseCase {

    LeaveRequestResult rejectCancelLeaveRequest(Long leaveRequestId, String rejectionReason);
}
