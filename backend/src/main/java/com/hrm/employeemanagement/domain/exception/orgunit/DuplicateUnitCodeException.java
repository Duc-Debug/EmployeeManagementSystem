package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;
public class DuplicateUnitCodeException extends DomainException {
    public DuplicateUnitCodeException(String message) {
        super(message);
    }
}