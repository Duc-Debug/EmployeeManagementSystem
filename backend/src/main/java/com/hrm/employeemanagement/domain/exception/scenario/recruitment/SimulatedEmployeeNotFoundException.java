package com.hrm.employeemanagement.domain.exception.scenario.recruitment;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class SimulatedEmployeeNotFoundException extends DomainException {
    public SimulatedEmployeeNotFoundException(Long employeeId) {
        super("Không tìm thấy nhân sự giả định với ID: " + employeeId);
    }

    public SimulatedEmployeeNotFoundException(String message) {
        super(message);
    }
}
