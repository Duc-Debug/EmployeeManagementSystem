package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ScenarioBaselineStaleException extends DomainException {
    public ScenarioBaselineStaleException(String message) {
        super(message);
    }
}

