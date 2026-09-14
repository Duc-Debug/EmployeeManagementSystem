package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ném ra khi không tìm thấy kỳ kế hoạch phân bổ tương ứng với ID.
 */
public class AllocationPeriodNotFoundException extends DomainException {

    public AllocationPeriodNotFoundException(String message) {
        super(message);
    }

    public AllocationPeriodNotFoundException(Long periodId) {
        super("Không tìm thấy kỳ kế hoạch phân bổ với ID: " + periodId);
    }
}
