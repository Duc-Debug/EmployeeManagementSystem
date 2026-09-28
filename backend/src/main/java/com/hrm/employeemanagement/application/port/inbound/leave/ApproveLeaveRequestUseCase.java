package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;

public interface ApproveLeaveRequestUseCase {
    LeaveRequestResult approveLeaveRequest(Long leaveRequestId, String approverComment);
}
