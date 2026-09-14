package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class AllocationNotFoundException extends DomainException {

    private final Long allocationId;

    public AllocationNotFoundException(Long allocationId) {
        super("Không tìm thấy dòng phân bổ nguồn lực với ID: " + allocationId);
        this.allocationId = allocationId;
    }

    public Long getAllocationId() {
        return allocationId;
    }
}
