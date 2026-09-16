package com.hrm.employeemanagement.domain.exception.scenario.recruitment;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ScenarioNotFoundException extends DomainException {
    public ScenarioNotFoundException(Long scenarioId) {
        super("Không tìm thấy kịch bản mô phỏng với ID: " + scenarioId);
    }

    public ScenarioNotFoundException(String message) {
        super(message);
    }
}
