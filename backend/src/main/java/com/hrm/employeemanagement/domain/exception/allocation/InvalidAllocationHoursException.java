package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidAllocationHoursException extends DomainException {

    public InvalidAllocationHoursException(String message) {
        super(message);
    }
}
