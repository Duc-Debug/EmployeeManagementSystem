package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.CreateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.CreateWorkLogUseCase;

public class TransactionalCreateWorkLogUseCase implements CreateWorkLogUseCase {

    private final CreateWorkLogUseCase delegate;

    public TransactionalCreateWorkLogUseCase(CreateWorkLogUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CreateWorkLogUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public WorkLogResult createWorkLog(CreateWorkLogCommand command) {
        return delegate.createWorkLog(command);
    }
}
