package com.hrm.employeemanagement.domain.timesheet;

/** Lifecycle state of a timesheet reminder. */
public enum ReminderStatus {
    PENDING,
    RETRY_PENDING,
    SENT,
    FAILED
}
