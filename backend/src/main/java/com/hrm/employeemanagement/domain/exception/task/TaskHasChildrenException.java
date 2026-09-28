package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class TaskHasChildrenException extends DomainException {
    public TaskHasChildrenException(Long taskId) {
        super("Không thể xóa hạng mục (ID: " + taskId + ") vì vẫn còn các công việc con trực thuộc");
    }
}
