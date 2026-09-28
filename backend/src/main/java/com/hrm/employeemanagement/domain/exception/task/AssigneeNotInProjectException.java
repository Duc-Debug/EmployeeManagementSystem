package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class AssigneeNotInProjectException extends DomainException {
    public AssigneeNotInProjectException(Long employeeId, Long projectId) {
        super("Nhân viên (ID: " + employeeId + ") không thuộc danh sách thành viên của dự án (ID: " + projectId + ")");
    }
}
