package com.hrm.employeemanagement.application.port.inbound.timesheet;

import java.time.LocalDate;

import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;

public interface GetWeeklyTimesheetUseCase {
    WeeklyTimesheetResult getMyWeeklyTimesheet(LocalDate dateInWeek);
}
