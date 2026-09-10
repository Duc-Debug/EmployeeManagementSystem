package com.hrm.employeemanagement.domain.exception.milestone;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class MilestoneNotFoundException extends DomainException {
    public MilestoneNotFoundException(Long milestoneId) {
        super("Không tìm thấy mốc tiến độ với ID: " + milestoneId);
    }
}
