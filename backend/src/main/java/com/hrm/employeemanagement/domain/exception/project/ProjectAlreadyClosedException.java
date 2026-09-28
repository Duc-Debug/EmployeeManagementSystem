package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectAlreadyClosedException extends DomainException {
    public ProjectAlreadyClosedException(String message) {
        super(message);
    }
}