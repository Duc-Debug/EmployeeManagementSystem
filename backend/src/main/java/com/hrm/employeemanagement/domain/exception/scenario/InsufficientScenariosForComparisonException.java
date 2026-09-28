package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InsufficientScenariosForComparisonException extends DomainException {
    public InsufficientScenariosForComparisonException() {
        super("Cần ít nhất hai kịch bản để so sánh");
    }

    public InsufficientScenariosForComparisonException(String message) {
        super(message);
    }
}
