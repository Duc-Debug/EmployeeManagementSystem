package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ném ra khi thao tác không phù hợp với trạng thái hiện tại của kỳ kế hoạch (ví dụ cố khóa kỳ đã khóa hoặc mở kỳ đang mở).
 */
public class InvalidAllocationPeriodStateException extends DomainException {

    public InvalidAllocationPeriodStateException(String message) {
        super(message);
    }
}
