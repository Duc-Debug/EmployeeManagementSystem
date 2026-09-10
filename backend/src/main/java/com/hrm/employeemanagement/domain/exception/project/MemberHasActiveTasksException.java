package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class MemberHasActiveTasksException extends DomainException {
    public MemberHasActiveTasksException(Long employeeId, Long projectId) {
        super("Không thể xóa nhân viên (ID: " + employeeId + ") đang phụ trách công việc chưa hoàn thành trong dự án (ID: " + projectId + "). Vui lòng bàn giao hoặc hoàn thành công việc trước khi xóa.");
    }
}
