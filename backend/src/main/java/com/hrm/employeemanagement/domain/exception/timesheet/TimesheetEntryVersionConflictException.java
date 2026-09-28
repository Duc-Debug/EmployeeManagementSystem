package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ khi phiên bản (version) của dòng giờ công bị xung đột (Optimistic Locking).
 */
public class TimesheetEntryVersionConflictException extends DomainException {
    public TimesheetEntryVersionConflictException(String message) {
        super(message);
    }
}
