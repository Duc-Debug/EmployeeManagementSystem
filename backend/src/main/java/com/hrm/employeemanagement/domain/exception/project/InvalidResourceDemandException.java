package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;
public class InvalidResourceDemandException extends DomainException {
    public InvalidResourceDemandException(String message) {
        super(message);
    }
}