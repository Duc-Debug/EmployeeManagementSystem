package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ScenarioAlreadyAppliedException extends DomainException {
    public ScenarioAlreadyAppliedException(String message) {
        super(message);
    }
}

