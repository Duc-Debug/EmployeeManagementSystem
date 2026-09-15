package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.SaveWeeklyTimesheetGridCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;

public interface SaveWeeklyTimesheetGridUseCase {
    WeeklyTimesheetResult saveWeeklyGrid(SaveWeeklyTimesheetGridCommand command);
}

