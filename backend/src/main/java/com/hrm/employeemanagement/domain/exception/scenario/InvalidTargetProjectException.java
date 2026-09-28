package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidTargetProjectException extends DomainException {
    public InvalidTargetProjectException(String message) {
        super(message);
    }
}

