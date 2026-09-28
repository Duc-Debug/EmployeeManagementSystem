package com.hrm.employeemanagement.application.port.outbound.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;

public interface LoadTimesheetEntryPort {
    Optional<TimesheetEntry> findById(TimesheetEntryId id);
    List<TimesheetEntry> findByTimesheetId(TimesheetId timesheetId);
    List<TimesheetEntry> findByEmployeeAndDateRange(EmployeeId employeeId, LocalDate startDate, LocalDate endDate);
    
    List<TimesheetEntry> findPendingApprovals(EmployeeId managerId);

    BigDecimal sumHoursByEmployeeAndDate(EmployeeId employeeId, LocalDate workDate, TimesheetEntryId excludeEntryId);
}
