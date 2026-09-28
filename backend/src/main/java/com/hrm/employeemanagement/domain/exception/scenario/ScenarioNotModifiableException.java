package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ScenarioNotModifiableException extends DomainException {
    public ScenarioNotModifiableException(String message) {
        super(message);
    }
}
