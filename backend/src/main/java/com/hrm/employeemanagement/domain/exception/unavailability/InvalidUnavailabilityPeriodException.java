package com.hrm.employeemanagement.domain.exception.unavailability;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidUnavailabilityPeriodException extends DomainException {
    public InvalidUnavailabilityPeriodException(String message) {
        super(message);
    }
}
