package com.hrm.employeemanagement.application.port.outbound.timesheet;

import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;

public interface DeleteTimesheetEntryPort {
    void deleteById(TimesheetEntryId id);
}
