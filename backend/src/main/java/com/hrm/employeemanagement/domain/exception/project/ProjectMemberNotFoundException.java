package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectMemberNotFoundException extends DomainException {
    public ProjectMemberNotFoundException(Long employeeId, Long projectId) {
        super("Nhân viên (ID: " + employeeId + ") không tồn tại trong danh sách thành viên của dự án (ID: " + projectId + ")");
    }
}
