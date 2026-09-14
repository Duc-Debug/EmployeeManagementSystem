package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Không có quyền
 * WorkLogTaskNotAssignedException
 */
public class WorkLogTaskNotAssignedException extends DomainException {
    public WorkLogTaskNotAssignedException(String message) {
        super(message);
    }
}
