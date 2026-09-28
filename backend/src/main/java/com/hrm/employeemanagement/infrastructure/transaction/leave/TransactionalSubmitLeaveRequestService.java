package com.hrm.employeemanagement.infrastructure.transaction.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;
import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalSubmitLeaveRequestService implements SubmitLeaveRequestUseCase {

    private final SubmitLeaveRequestUseCase delegate;

    public TransactionalSubmitLeaveRequestService(SubmitLeaveRequestUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public LeaveRequestResult submitLeaveRequest(SubmitLeaveRequestCommand command) {
        return delegate.submitLeaveRequest(command);
    }
}
