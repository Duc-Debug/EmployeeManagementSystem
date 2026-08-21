package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class CyclicDependencyException extends DomainException {
    public CyclicDependencyException(String message) {
        super(message);
    }
}
