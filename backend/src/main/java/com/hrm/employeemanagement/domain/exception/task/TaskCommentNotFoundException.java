package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class TaskCommentNotFoundException extends DomainException {
    public TaskCommentNotFoundException(String message) {
        super(message);
    }
}

