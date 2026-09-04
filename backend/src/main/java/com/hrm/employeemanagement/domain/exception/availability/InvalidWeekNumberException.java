package com.hrm.employeemanagement.domain.exception.availability;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidWeekNumberException extends DomainException {
    public InvalidWeekNumberException(String message) {
        super(message);
    }
}
