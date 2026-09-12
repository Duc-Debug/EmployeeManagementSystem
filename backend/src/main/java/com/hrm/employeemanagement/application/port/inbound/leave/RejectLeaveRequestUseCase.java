package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;

public interface RejectLeaveRequestUseCase {
    LeaveRequestResult rejectLeaveRequest(Long leaveRequestId, String rejectionReason);
}
