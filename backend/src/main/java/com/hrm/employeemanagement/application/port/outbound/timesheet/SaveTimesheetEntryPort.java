package com.hrm.employeemanagement.application.port.outbound.timesheet;

import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;

public interface SaveTimesheetEntryPort {
    TimesheetEntry save(TimesheetEntry entry);
}
