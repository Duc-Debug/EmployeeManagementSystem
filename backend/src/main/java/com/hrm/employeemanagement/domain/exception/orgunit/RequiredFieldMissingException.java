package com.hrm.employeemanagement.domain.exception.orgunit;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class RequiredFieldMissingException extends DomainException {
    public RequiredFieldMissingException(String message) {
        super(message);
    }

    public static RequiredFieldMissingException of(String fieldDisplayName) {
        return new RequiredFieldMissingException(fieldDisplayName + " không được để trống.");
    }
}
