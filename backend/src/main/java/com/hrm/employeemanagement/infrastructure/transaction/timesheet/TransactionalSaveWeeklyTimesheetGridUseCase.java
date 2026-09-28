package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.SaveWeeklyTimesheetGridCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SaveWeeklyTimesheetGridUseCase;

public class TransactionalSaveWeeklyTimesheetGridUseCase implements SaveWeeklyTimesheetGridUseCase {

    private final SaveWeeklyTimesheetGridUseCase delegate;

    public TransactionalSaveWeeklyTimesheetGridUseCase(SaveWeeklyTimesheetGridUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "SaveWeeklyTimesheetGridUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public WeeklyTimesheetResult saveWeeklyGrid(SaveWeeklyTimesheetGridCommand command) {
        return delegate.saveWeeklyGrid(command);
    }
}

