package com.hrm.employeemanagement.domain.exception.availability;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidAvailabilityHoursException extends DomainException {
    public InvalidAvailabilityHoursException(String message) {
        super(message);
    }
}
