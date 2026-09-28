package com.hrm.employeemanagement.domain.exception.task;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class TaskAttachmentNotFoundException extends DomainException {
    public TaskAttachmentNotFoundException(Long attachmentId) {
        super("Không tìm thấy tệp đính kèm với ID: " + attachmentId);
    }
}

