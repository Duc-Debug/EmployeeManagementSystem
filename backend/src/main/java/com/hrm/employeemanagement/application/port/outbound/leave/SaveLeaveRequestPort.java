package com.hrm.employeemanagement.application.port.outbound.leave;

import com.hrm.employeemanagement.domain.leave.LeaveRequest;

public interface SaveLeaveRequestPort {
    LeaveRequest save(LeaveRequest leaveRequest);
}
