package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.port.inbound.leave.CancelLeaveRequestUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Decorator pattern quản lý Transaction cho Use Case Hủy đơn nghỉ phép.
 */
public class TransactionalCancelLeaveRequestService implements CancelLeaveRequestUseCase {

    private final CancelLeaveRequestUseCase target;

    public TransactionalCancelLeaveRequestService(CancelLeaveRequestUseCase target) {
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeaveRequest(Long leaveRequestId) {
        target.cancelLeaveRequest(leaveRequestId);
    }
}
