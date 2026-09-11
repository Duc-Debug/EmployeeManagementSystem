package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.ApproveLeaveRequestUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalApproveLeaveRequestService implements ApproveLeaveRequestUseCase {

    private final ApproveLeaveRequestUseCase delegate;

    public TransactionalApproveLeaveRequestService(ApproveLeaveRequestUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public LeaveRequestResult approveLeaveRequest(Long leaveRequestId, String approverComment) {
        return delegate.approveLeaveRequest(leaveRequestId, approverComment);
    }
}
