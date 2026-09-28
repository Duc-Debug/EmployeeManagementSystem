package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ném ra khi dữ liệu kỳ kế hoạch không hợp lệ (ví dụ dải tuần không hợp lệ).
 */
public class InvalidAllocationPeriodException extends DomainException {

    public InvalidAllocationPeriodException(String message) {
        super(message);
    }
}
