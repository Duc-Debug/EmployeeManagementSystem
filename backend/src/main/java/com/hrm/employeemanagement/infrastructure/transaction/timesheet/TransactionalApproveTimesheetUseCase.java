package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.ApproveTimesheetUseCase;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalApproveTimesheetUseCase implements ApproveTimesheetUseCase {
    private final ApproveTimesheetUseCase delegate;

    public TransactionalApproveTimesheetUseCase(ApproveTimesheetUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ApprovalResult approveEntry(Long entryId, Long version) {
        return delegate.approveEntry(entryId, version);
    }

    @Override
    @Transactional
    public ApprovalResult rejectEntry(Long entryId, Long version, String reason) {
        return delegate.rejectEntry(entryId, version, reason);
    }
}
