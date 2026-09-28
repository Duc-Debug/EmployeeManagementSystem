package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidAllocationPercentageException extends DomainException {

    public InvalidAllocationPercentageException(String message) {
        super(message);
    }
}
