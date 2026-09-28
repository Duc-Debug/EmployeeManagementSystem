package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;

/**
 * NCL-05-CN-007: Nhân viên chuyên môn gửi yêu cầu hủy đơn nghỉ phép đã duyệt.
 */
public interface RequestCancelApprovedLeaveUseCase {

    LeaveRequestResult requestCancelApprovedLeave(Long leaveRequestId, String reason);
}
