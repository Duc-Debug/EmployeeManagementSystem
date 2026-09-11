package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;

import java.util.List;

public interface GetPendingLeaveRequestsUseCase {
    List<LeaveRequestResult> getPendingLeaveRequests();
}
