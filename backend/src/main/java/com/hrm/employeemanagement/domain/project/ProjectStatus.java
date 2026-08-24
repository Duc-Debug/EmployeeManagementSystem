package com.hrm.employeemanagement.domain.project;

public enum ProjectStatus {
    ACTIVE,
    INACTIVE;

    public static ProjectStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }

        return ProjectStatus.valueOf(value.trim().toUpperCase());
    }
}
