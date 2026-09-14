package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Giờ làm đã duyệt là bất biến
 * TimesheetImmutableException
 */
public class TimesheetImmutableException extends DomainException {
    public TimesheetImmutableException(String message) {
        super(message);
    }
}
