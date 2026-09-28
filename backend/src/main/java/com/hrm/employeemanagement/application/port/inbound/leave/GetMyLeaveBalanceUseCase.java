package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;

public interface GetMyLeaveBalanceUseCase {
    LeaveBalanceResult getMyLeaveBalance(Integer year);
}
