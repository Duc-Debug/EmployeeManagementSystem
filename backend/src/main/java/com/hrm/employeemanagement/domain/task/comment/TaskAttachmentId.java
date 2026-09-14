package com.hrm.employeemanagement.domain.task.comment;

import java.util.Objects;

public record TaskAttachmentId(Long value) {
    public TaskAttachmentId {
        Objects.requireNonNull(value, "TaskAttachmentId không được null");
    }

    public static TaskAttachmentId of(Long value) {
        return value != null ? new TaskAttachmentId(value) : null;
    }
}

