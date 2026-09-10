package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectNotClosedException extends DomainException {
    public ProjectNotClosedException(String message) {
        super(message);
    }
}