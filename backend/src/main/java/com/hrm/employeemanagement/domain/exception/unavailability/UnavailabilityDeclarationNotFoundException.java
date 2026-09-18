package com.hrm.employeemanagement.domain.exception.unavailability;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class UnavailabilityDeclarationNotFoundException extends DomainException {
    public UnavailabilityDeclarationNotFoundException(String message) {
        super(message);
    }
}
