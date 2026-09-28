package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class NullOrgUnitIdException extends DomainException {
    public NullOrgUnitIdException(String message) {
        super(message);
    }
}
