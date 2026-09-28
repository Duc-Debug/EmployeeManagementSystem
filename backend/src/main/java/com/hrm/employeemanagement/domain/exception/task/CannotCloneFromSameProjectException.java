package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class CannotCloneFromSameProjectException extends DomainException {

    public CannotCloneFromSameProjectException() {
        super("Không thể nhân bản cây công việc từ chính dự án này");
    }
}
