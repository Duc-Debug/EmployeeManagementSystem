package com.hrm.employeemanagement.domain.exception.timesheet;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ khi lý do điều chỉnh giờ làm đã duyệt bị để trống hoặc không hợp lệ.
 */
public class WorkLogAdjustmentReasonRequiredException extends DomainException {
    public WorkLogAdjustmentReasonRequiredException(String message) {
        super(message);
    }
}
