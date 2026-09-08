package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class AllocationCapacityExceededException extends DomainException {

    public AllocationCapacityExceededException(String message) {
        super(message);
    }
}
