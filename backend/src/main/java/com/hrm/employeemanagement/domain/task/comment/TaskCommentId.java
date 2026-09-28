package com.hrm.employeemanagement.domain.task.comment;

import java.util.Objects;

public record TaskCommentId(Long value) {
    public TaskCommentId {
        Objects.requireNonNull(value, "TaskCommentId không được null");
    }

    public static TaskCommentId of(Long value) {
        return value != null ? new TaskCommentId(value) : null;
    }
}

