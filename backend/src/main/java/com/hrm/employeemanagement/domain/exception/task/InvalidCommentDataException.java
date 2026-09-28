package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidCommentDataException extends DomainException {
    public InvalidCommentDataException(String message) {
        super(message);
    }
}

