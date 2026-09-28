package com.hrm.employeemanagement.application.port.outbound.timesheet;

import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;

public interface SaveTimesheetEntryPort {

    /**
     * Persists an entry while enforcing the shared 12-hour/day invariant.
     * Implementations must serialize on the employee aggregate before re-reading
     * the daily total; callers must not be able to bypass that locking contract.
     */
    TimesheetEntry save(TimesheetEntry entry);
}
