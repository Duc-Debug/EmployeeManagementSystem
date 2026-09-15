package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ScenarioNotFoundException extends DomainException {
    public ScenarioNotFoundException(Long id) {
        super("Không tìm thấy kịch bản mô phỏng với ID: " + id);
    }

    public ScenarioNotFoundException(String message) {
        super(message);
    }
}
