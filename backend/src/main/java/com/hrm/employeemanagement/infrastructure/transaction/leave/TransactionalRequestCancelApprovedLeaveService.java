package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.RequestCancelApprovedLeaveUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalRequestCancelApprovedLeaveService implements RequestCancelApprovedLeaveUseCase {

    private final RequestCancelApprovedLeaveUseCase delegate;

    public TransactionalRequestCancelApprovedLeaveService(RequestCancelApprovedLeaveUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public LeaveRequestResult requestCancelApprovedLeave(Long leaveRequestId, String reason) {
        return delegate.requestCancelApprovedLeave(leaveRequestId, reason);
    }
}
