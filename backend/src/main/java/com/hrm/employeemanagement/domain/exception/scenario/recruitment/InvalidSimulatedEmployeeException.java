package com.hrm.employeemanagement.domain.exception.scenario.recruitment;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidSimulatedEmployeeException extends DomainException {
    public InvalidSimulatedEmployeeException(String message) {
        super(message);
    }
}
