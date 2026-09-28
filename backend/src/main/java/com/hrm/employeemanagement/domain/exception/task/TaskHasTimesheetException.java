package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class TaskHasTimesheetException extends DomainException {
    public TaskHasTimesheetException(Long taskId) {
        super("Không thể xóa công việc (ID: " + taskId + ") vì đã phát sinh giờ công chấm công thực tế");
    }
}
