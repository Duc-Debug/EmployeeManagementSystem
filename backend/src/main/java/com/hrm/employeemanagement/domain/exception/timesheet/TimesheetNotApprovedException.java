package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ khi cố gắng điều chỉnh dòng giờ công chưa được duyệt.
 */
public class TimesheetNotApprovedException extends DomainException {
    public TimesheetNotApprovedException(String message) {
        super(message);
    }
}
