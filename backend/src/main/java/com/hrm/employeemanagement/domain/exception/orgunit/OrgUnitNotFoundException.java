package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;
public class OrgUnitNotFoundException extends DomainException {
    public OrgUnitNotFoundException(String message) {
        super(message);
    }
}
