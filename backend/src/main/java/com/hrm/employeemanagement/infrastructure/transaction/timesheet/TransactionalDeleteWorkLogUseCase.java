package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.DeleteWorkLogCommand;
import com.hrm.employeemanagement.application.port.inbound.timesheet.DeleteWorkLogUseCase;

public class TransactionalDeleteWorkLogUseCase implements DeleteWorkLogUseCase {

    private final DeleteWorkLogUseCase delegate;

    public TransactionalDeleteWorkLogUseCase(DeleteWorkLogUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "DeleteWorkLogUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public void deleteWorkLog(DeleteWorkLogCommand command) {
        delegate.deleteWorkLog(command);
    }
}
