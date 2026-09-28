package com.hrm.employeemanagement.domain.exception.workweek;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class StandardWorkWeekVersionConflictException extends DomainException {
    public StandardWorkWeekVersionConflictException(String message) {
        super(message);
    }
}
