package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetEmployeeLeaveBalanceUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Decorator quản lý Transaction cho Use Case xem quỹ phép theo ID nhân viên (NCL-05-CN-005).
 */
public class TransactionalGetEmployeeLeaveBalanceService implements GetEmployeeLeaveBalanceUseCase {

    private final GetEmployeeLeaveBalanceUseCase target;

    public TransactionalGetEmployeeLeaveBalanceService(GetEmployeeLeaveBalanceUseCase target) {
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveBalanceResult getEmployeeLeaveBalance(Long employeeId, Integer year) {
        return target.getEmployeeLeaveBalance(employeeId, year);
    }
}
