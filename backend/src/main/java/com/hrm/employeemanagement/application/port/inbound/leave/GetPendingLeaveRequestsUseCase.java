package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;

import java.util.List;

public interface GetPendingLeaveRequestsUseCase {
    List<LeaveRequestResult> getPendingLeaveRequests();

    PageResult<LeaveRequestResult> getPendingLeaveRequests(int page, int size);
}
