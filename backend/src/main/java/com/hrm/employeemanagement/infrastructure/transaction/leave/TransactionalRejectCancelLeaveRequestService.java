package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.RejectCancelLeaveRequestUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalRejectCancelLeaveRequestService implements RejectCancelLeaveRequestUseCase {

    private final RejectCancelLeaveRequestUseCase delegate;

    public TransactionalRejectCancelLeaveRequestService(RejectCancelLeaveRequestUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public LeaveRequestResult rejectCancelLeaveRequest(Long leaveRequestId, String rejectionReason) {
        return delegate.rejectCancelLeaveRequest(leaveRequestId, rejectionReason);
    }
}
