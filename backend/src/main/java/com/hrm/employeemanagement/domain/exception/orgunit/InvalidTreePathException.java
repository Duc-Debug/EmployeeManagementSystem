package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidTreePathException extends DomainException {
    public InvalidTreePathException(String message) {
        super(message);
    }
}
