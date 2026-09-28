package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.UpdateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.UpdateWorkLogUseCase;

public class TransactionalUpdateWorkLogUseCase implements UpdateWorkLogUseCase {

    private final UpdateWorkLogUseCase delegate;

    public TransactionalUpdateWorkLogUseCase(UpdateWorkLogUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "UpdateWorkLogUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public WorkLogResult updateWorkLog(UpdateWorkLogCommand command) {
        return delegate.updateWorkLog(command);
    }
}
