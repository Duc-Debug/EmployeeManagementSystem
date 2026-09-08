package com.hrm.employeemanagement.domain.task;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public static TaskStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return TODO;
        }
        return TaskStatus.valueOf(value.trim().toUpperCase());
    }
}
