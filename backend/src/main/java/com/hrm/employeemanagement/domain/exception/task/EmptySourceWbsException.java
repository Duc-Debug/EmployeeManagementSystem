package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class EmptySourceWbsException extends DomainException {

    public EmptySourceWbsException(Long sourceProjectId) {
        super("Dự án nguồn (ID: " + sourceProjectId + ") chưa có cây công việc nào để nhân bản");
    }
}
