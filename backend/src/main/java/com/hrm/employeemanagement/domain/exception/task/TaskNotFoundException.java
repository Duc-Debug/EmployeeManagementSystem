package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class TaskNotFoundException extends DomainException {
    public TaskNotFoundException(Long taskId) {
        super("Không tìm thấy hạng mục/công việc với ID: " + taskId);
    }
}
