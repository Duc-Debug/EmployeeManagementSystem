package com.hrm.employeemanagement.domain.exception.employee;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class EmployeeNotFoundException extends DomainException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
