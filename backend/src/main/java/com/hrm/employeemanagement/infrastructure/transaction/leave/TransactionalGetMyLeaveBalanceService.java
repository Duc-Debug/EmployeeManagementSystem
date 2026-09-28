package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetMyLeaveBalanceUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Decorator quản lý Transaction cho Use Case xem quỹ phép của chính mình (NCL-05-CN-005).
 */
public class TransactionalGetMyLeaveBalanceService implements GetMyLeaveBalanceUseCase {

    private final GetMyLeaveBalanceUseCase target;

    public TransactionalGetMyLeaveBalanceService(GetMyLeaveBalanceUseCase target) {
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveBalanceResult getMyLeaveBalance(Integer year) {
        return target.getMyLeaveBalance(year);
    }
}
