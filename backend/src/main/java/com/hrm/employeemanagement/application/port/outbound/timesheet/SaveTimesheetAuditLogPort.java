package com.hrm.employeemanagement.application.port.outbound.timesheet;

import com.hrm.employeemanagement.domain.timesheet.TimesheetAuditLog;

public interface SaveTimesheetAuditLogPort {
    TimesheetAuditLog save(TimesheetAuditLog auditLog);
}
