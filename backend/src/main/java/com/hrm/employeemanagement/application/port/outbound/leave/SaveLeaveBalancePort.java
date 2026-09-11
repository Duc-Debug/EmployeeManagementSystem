package com.hrm.employeemanagement.application.port.outbound.leave;

import com.hrm.employeemanagement.domain.leave.LeaveBalance;

public interface SaveLeaveBalancePort {
    LeaveBalance save(LeaveBalance leaveBalance);
}
