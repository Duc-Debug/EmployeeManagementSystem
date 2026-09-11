package com.hrm.employeemanagement.application.port.outbound.leave;

import com.hrm.employeemanagement.domain.leave.LeaveBalance;

import java.util.Optional;

public interface LoadLeaveBalancePort {
    Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);
}
