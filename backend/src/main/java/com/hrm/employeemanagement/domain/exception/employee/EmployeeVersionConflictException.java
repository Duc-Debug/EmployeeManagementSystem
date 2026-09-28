package com.hrm.employeemanagement.domain.exception.employee;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class EmployeeVersionConflictException extends DomainException {
    public EmployeeVersionConflictException(String message) {
        super(message);
    }
}
