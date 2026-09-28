package com.hrm.employeemanagement.domain.exception.milestone;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class TaskNotInProjectException extends DomainException {
    public TaskNotInProjectException(Long taskId, Long projectId) {
        super(String.format("Công việc với ID %d không thuộc dự án có ID %d", taskId, projectId));
    }
}
