package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectClosedException extends DomainException {
    public ProjectClosedException(Long projectId) {
        super("Không thể thêm hoặc chỉnh sửa công việc trong dự án đã đóng (ID: " + projectId + ")");
    }

    public ProjectClosedException(String message) {
        super(message);
    }
}
