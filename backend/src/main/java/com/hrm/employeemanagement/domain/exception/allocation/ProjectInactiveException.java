package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectInactiveException extends DomainException {

    public ProjectInactiveException(String message) {
        super(message);
    }
}
