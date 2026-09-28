package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.AssignedTaskOptionResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetMyAssignedTasksForWorkLogUseCase;

public class TransactionalGetMyAssignedTasksForWorkLogUseCase implements GetMyAssignedTasksForWorkLogUseCase {

    private final GetMyAssignedTasksForWorkLogUseCase delegate;

    public TransactionalGetMyAssignedTasksForWorkLogUseCase(GetMyAssignedTasksForWorkLogUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetMyAssignedTasksForWorkLogUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskOptionResult> getMyAssignedTasksForWorkLog() {
        return delegate.getMyAssignedTasksForWorkLog();
    }
}
