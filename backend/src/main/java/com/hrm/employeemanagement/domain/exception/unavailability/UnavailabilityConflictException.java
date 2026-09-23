package com.hrm.employeemanagement.domain.exception.unavailability;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class UnavailabilityConflictException extends DomainException {
    public UnavailabilityConflictException(String message) {
        super(message);
    }
}
