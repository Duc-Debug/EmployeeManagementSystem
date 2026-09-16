package com.hrm.employeemanagement.application.port.inbound.timesheet;

/** Outcome of one queue page, separated from the number of reminders sent. */
public record ReminderBatchResult(
        int loaded,
        int sent,
        int retryScheduled,
        int permanentlyFailed) {

    public static ReminderBatchResult empty() {
        return new ReminderBatchResult(0, 0, 0, 0);
    }
}
