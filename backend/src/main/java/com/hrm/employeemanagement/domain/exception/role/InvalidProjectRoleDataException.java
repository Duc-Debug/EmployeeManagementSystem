package com.hrm.employeemanagement.domain.exception.role;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidProjectRoleDataException extends DomainException {
    public InvalidProjectRoleDataException(String message) {
        super(message);
    }
}
