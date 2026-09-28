package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Thiếu dữ liệu
 * WorkLogDescriptionBlankException
 */
public class WorkLogDescriptionBlankException extends DomainException {
    public WorkLogDescriptionBlankException(String message) {
        super(message);
    }
}
