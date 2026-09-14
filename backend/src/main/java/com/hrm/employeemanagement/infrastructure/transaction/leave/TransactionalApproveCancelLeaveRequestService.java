package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.ApproveCancelLeaveRequestUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalApproveCancelLeaveRequestService implements ApproveCancelLeaveRequestUseCase {

    private final ApproveCancelLeaveRequestUseCase delegate;

    public TransactionalApproveCancelLeaveRequestService(ApproveCancelLeaveRequestUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public LeaveRequestResult approveCancelLeaveRequest(Long leaveRequestId, String approverComment) {
        return delegate.approveCancelLeaveRequest(leaveRequestId, approverComment);
    }
}
