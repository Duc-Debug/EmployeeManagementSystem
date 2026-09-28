package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidRoleAllocationTemplateException extends DomainException {
    public InvalidRoleAllocationTemplateException(String message) {
        super(message);
    }
}

