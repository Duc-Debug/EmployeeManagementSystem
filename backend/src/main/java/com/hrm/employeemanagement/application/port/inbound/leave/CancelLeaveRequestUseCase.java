package com.hrm.employeemanagement.application.port.inbound.leave;

public interface CancelLeaveRequestUseCase {
    void cancelLeaveRequest(Long leaveRequestId);
}
