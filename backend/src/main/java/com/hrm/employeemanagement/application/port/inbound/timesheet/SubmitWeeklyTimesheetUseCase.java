package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.SubmitWeeklyTimesheetCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;

public interface SubmitWeeklyTimesheetUseCase {
    WeeklyTimesheetResult submitWeeklyTimesheet(SubmitWeeklyTimesheetCommand command);
}