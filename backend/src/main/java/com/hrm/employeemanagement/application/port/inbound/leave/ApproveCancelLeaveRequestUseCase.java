package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;

/**
 * NCL-05-CN-007: Quản lý nguồn lực phê duyệt yêu cầu hủy đơn nghỉ phép đã duyệt.
 */
public interface ApproveCancelLeaveRequestUseCase {

    LeaveRequestResult approveCancelLeaveRequest(Long leaveRequestId, String approverComment);
}
