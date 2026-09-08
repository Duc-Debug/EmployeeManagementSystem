package com.hrm.employeemanagement.domain.task;

import java.util.Objects;

public record TaskId(Long value) {
    public TaskId {
        Objects.requireNonNull(value, "TaskId không được null");
    }

    public static TaskId of(Long value) {
        return value != null ? new TaskId(value) : null;
    }
}
