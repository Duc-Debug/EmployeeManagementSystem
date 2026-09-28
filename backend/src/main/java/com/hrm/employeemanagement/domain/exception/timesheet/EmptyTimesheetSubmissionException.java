package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Báo lỗi khi nộp bảng chấm công không có dòng ghi giờ nào
 */
public class EmptyTimesheetSubmissionException extends DomainException {
    public EmptyTimesheetSubmissionException(String message) {
        super(message);
    }
}