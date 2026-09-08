package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class EmployeeInactiveException extends DomainException {

    public EmployeeInactiveException(String message) {
        super(message);
    }
}
