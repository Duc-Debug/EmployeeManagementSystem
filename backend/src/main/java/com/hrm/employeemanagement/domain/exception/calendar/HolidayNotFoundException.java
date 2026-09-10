package com.hrm.employeemanagement.domain.exception.calendar;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class HolidayNotFoundException extends DomainException {
    public HolidayNotFoundException(String message) {
        super(message);
    }
}
