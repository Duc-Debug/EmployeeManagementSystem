package com.hrm.employeemanagement.domain.exception.employee;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidEmployeeDataException extends DomainException {
    public InvalidEmployeeDataException(String message) {
        super(message);
    }

    public InvalidEmployeeDataException(String message, Throwable cause) {
        super(message, cause);
    }
}