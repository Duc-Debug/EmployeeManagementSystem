package com.hrm.employeemanagement.domain.task;

public enum TaskType {
    CATEGORY,
    TASK;

    public static TaskType fromString(String value) {
        if (value == null || value.isBlank()) {
            return TASK;
        }
        return TaskType.valueOf(value.trim().toUpperCase());
    }
}
