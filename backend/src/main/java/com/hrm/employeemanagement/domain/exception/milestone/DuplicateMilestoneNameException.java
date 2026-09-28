package com.hrm.employeemanagement.domain.exception.milestone;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateMilestoneNameException extends DomainException {
    public DuplicateMilestoneNameException(String name) {
        super("Tên mốc tiến độ đã tồn tại trong dự án: " + name);
    }
}
