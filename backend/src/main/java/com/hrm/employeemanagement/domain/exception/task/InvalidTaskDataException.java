package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidTaskDataException extends DomainException {
    public InvalidTaskDataException(String message) {
        super(message);
    }
}
