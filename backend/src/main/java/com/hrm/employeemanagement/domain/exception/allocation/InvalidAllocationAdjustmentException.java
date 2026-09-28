package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidAllocationAdjustmentException extends DomainException {

    public InvalidAllocationAdjustmentException(String message) {
        super(message);
    }
}
