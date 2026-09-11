package com.hrm.employeemanagement.domain.exception.leave;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * TC-03: Ngoại lệ ném ra khi khoảng ngày xin nghỉ không hợp lệ.
 */
public class InvalidLeaveDateRangeException extends DomainException {

    public InvalidLeaveDateRangeException(String message) {
        super(message);
    }

    public static InvalidLeaveDateRangeException endBeforeStart() {
        return new InvalidLeaveDateRangeException("Ngày kết thúc không được sớm hơn ngày bắt đầu");
    }

    public static InvalidLeaveDateRangeException emptyDates() {
        return new InvalidLeaveDateRangeException("Ngày bắt đầu và ngày kết thúc không được để trống");
    }
}
