package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ScenarioDemandNotFoundException extends DomainException {
    public ScenarioDemandNotFoundException(Long demandId) {
        super("Không tìm thấy nhu cầu nhân sự giả định với ID: " + demandId);
    }
}
