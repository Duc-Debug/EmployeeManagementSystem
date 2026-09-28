package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class AssigneeInactiveException extends DomainException {
    public AssigneeInactiveException(String message) {
        super(message);
    }
}

