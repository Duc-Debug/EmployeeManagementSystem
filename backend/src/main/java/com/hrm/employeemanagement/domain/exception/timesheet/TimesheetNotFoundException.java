package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Không tìm thấy bảng chấm công 
 * TimesheetNotFoundException
 */
public class TimesheetNotFoundException extends DomainException {
    public TimesheetNotFoundException(String message) {
        super(message);
    }

    public TimesheetNotFoundException(Long id) {
        super("Không tìm thấy bảng chấm công với ID: " + id);
    }
}
