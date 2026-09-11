package com.hrm.employeemanagement.domain.exception.role;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidProjectRoleStateException extends DomainException {
    public InvalidProjectRoleStateException(String message) {
        super(message);
    }
}
