package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateScenarioCodeException extends DomainException {
    public DuplicateScenarioCodeException(String code) {
        super("Mã kịch bản đã tồn tại: " + code);
    }
}
