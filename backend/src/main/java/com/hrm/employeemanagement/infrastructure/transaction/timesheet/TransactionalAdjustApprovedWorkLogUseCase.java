package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.AdjustApprovedWorkLogUseCase;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalAdjustApprovedWorkLogUseCase implements AdjustApprovedWorkLogUseCase {
    private final AdjustApprovedWorkLogUseCase delegate;

    public TransactionalAdjustApprovedWorkLogUseCase(AdjustApprovedWorkLogUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public AdjustApprovedWorkLogResult adjustApprovedWorkLog(AdjustApprovedWorkLogCommand command) {
        return delegate.adjustApprovedWorkLog(command);
    }
}
