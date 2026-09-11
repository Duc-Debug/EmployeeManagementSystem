package com.hrm.employeemanagement.domain.exception.leave;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * TC-02: Ngoại lệ ném ra khi nhân viên đã có đơn nghỉ trùng khoảng ngày.
 */
public class DuplicateLeaveRequestException extends DomainException {

    public DuplicateLeaveRequestException(String message) {
        super(message);
    }

    public static DuplicateLeaveRequestException overlapping() {
        return new DuplicateLeaveRequestException("Bạn đã có đơn nghỉ phép trong khoảng thời gian này");
    }
}
