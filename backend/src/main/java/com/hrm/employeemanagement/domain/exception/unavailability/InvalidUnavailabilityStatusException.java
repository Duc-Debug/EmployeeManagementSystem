package com.hrm.employeemanagement.domain.exception.unavailability;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidUnavailabilityStatusException extends DomainException {
    public InvalidUnavailabilityStatusException(String message) {
        super(message);
    }
}
