package com.hrm.employeemanagement.domain.timesheet;

public record TimesheetAuditLogId(Long value) {
    public TimesheetAuditLogId {
        if (value == null) {
            throw new IllegalArgumentException("TimesheetAuditLogId value must not be null");
        }
    }
}
