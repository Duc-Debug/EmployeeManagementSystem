package com.hrm.employeemanagement.application.port.outbound.timesheet;

import com.hrm.employeemanagement.domain.timesheet.Timesheet;

public interface SaveTimesheetPort {
    Timesheet save(Timesheet timesheet);
    void saveAll(java.util.List<Timesheet> timesheets);
}
