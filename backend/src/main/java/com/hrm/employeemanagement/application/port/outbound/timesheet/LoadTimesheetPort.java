package com.hrm.employeemanagement.application.port.outbound.timesheet;

import java.time.LocalDate;
import java.util.Optional;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;

public interface LoadTimesheetPort {
    Optional<Timesheet> findById(TimesheetId id);
    Optional<Timesheet> findByEmployeeAndWeekStart(EmployeeId employeeId, LocalDate weekStartDate);
    java.util.List<Timesheet> findDraftTimesheetsForReminderUpTo(LocalDate targetDate, int limit);
}
