package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Không ghi công vào dự án đã đóng
 * WorkLogInClosedProjectException
 */
public class WorkLogInClosedProjectException extends DomainException {
    public WorkLogInClosedProjectException(String message) {
        super(message);
    }
}
