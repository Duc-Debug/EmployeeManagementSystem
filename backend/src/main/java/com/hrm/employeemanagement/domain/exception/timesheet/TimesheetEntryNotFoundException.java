package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * Tài nguyên không tồn tại. 
 * TimesheetEntryNotFoundException
 */
public class TimesheetEntryNotFoundException extends DomainException {
    public TimesheetEntryNotFoundException(String message) {
        super(message);
    }

    public TimesheetEntryNotFoundException(Long id) {
        super("Không tìm thấy dòng ghi giờ với ID: " + id);
    }
}
