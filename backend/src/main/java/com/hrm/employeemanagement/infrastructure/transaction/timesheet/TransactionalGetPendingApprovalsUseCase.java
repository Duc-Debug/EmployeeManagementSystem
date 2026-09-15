package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetPendingApprovalsUseCase;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public class TransactionalGetPendingApprovalsUseCase implements GetPendingApprovalsUseCase {
    private final GetPendingApprovalsUseCase delegate;

    public TransactionalGetPendingApprovalsUseCase(GetPendingApprovalsUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkLogResult> getPendingApprovals() {
        return delegate.getPendingApprovals();
    }
}
