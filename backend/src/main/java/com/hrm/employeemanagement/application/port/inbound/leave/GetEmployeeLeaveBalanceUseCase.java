package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;

public interface GetEmployeeLeaveBalanceUseCase {
    LeaveBalanceResult getEmployeeLeaveBalance(Long employeeId, Integer year);
}
