package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateProjectMemberException extends DomainException {
    public DuplicateProjectMemberException(Long employeeId, Long projectId) {
        super("Nhân viên (ID: " + employeeId + ") đã là thành viên của dự án (ID: " + projectId + ")");
    }
}
