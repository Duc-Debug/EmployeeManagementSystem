package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidScenarioDemandException extends DomainException {
    public InvalidScenarioDemandException(String message) {
        super(message);
    }
}
