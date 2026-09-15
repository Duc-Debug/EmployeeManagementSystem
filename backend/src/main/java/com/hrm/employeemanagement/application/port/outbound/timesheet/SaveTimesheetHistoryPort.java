package com.hrm.employeemanagement.application.port.outbound.timesheet;

import com.hrm.employeemanagement.domain.timesheet.TimesheetHistory;

public interface SaveTimesheetHistoryPort {
    void save(TimesheetHistory history);
    void saveAll(java.util.List<TimesheetHistory> histories);
}
