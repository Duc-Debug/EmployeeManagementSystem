package com.hrm.employeemanagement.domain.exception.milestone;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidMilestoneDataException extends DomainException {
    public InvalidMilestoneDataException(String message) {
        super(message);
    }
}
