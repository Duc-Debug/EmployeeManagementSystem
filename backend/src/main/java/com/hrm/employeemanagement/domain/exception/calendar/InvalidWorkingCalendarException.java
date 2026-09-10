package com.hrm.employeemanagement.domain.exception.calendar;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidWorkingCalendarException extends DomainException {
    public InvalidWorkingCalendarException(String message) {
        super(message);
    }
}
