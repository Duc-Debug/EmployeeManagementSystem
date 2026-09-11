package com.hrm.employeemanagement.domain.exception.leave;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ném ra khi không tìm thấy đơn xin nghỉ phép trong hệ thống.
 */
public class LeaveRequestNotFoundException extends DomainException {

    public LeaveRequestNotFoundException(String message) {
        super(message);
    }
}
