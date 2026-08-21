package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InactiveParentException extends DomainException {
    public InactiveParentException(String message) {
        super(message);
    }
}
