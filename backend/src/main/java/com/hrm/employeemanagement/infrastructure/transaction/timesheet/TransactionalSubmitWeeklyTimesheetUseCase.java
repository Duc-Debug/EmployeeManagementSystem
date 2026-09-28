package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.SubmitWeeklyTimesheetCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SubmitWeeklyTimesheetUseCase;

public class TransactionalSubmitWeeklyTimesheetUseCase implements SubmitWeeklyTimesheetUseCase {

    private final SubmitWeeklyTimesheetUseCase delegate;

    public TransactionalSubmitWeeklyTimesheetUseCase(SubmitWeeklyTimesheetUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "SubmitWeeklyTimesheetUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public WeeklyTimesheetResult submitWeeklyTimesheet(SubmitWeeklyTimesheetCommand command) {
        return delegate.submitWeeklyTimesheet(command);
    }
}