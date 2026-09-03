package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class RequiredFieldMissingException extends DomainException {
    public RequiredFieldMissingException(String message) {
        super(message);
    }

    public static RequiredFieldMissingException of(String fieldName) {
        return new RequiredFieldMissingException("Trường bắt buộc '" + fieldName + "' không được để trống.");
    }
}
