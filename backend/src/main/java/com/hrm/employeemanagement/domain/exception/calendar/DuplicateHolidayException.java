package com.hrm.employeemanagement.domain.exception.calendar;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateHolidayException extends DomainException {
    public DuplicateHolidayException(String message) {
        super(message);
    }
}
