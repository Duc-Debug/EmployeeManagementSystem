package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Dữ liệu không hợp lệ
 * WorkLogInvalidHoursException
 */
public class WorkLogInvalidHoursException extends DomainException {
    public WorkLogInvalidHoursException(String message) {
        super(message);
    }
}
