package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetWeeklyTimesheetUseCase;

public class TransactionalGetWeeklyTimesheetUseCase implements GetWeeklyTimesheetUseCase {

    private final GetWeeklyTimesheetUseCase delegate;

    public TransactionalGetWeeklyTimesheetUseCase(GetWeeklyTimesheetUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "GetWeeklyTimesheetUseCase delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyTimesheetResult getMyWeeklyTimesheet(LocalDate dateInWeek) {
        return delegate.getMyWeeklyTimesheet(dateInWeek);
    }
}
