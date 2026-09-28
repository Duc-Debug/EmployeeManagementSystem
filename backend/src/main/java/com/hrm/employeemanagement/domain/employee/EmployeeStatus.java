package com.hrm.employeemanagement.domain.employee;

/**
 * Domain Enum representing the lifecycle status of an Employee.
 */
public enum EmployeeStatus {
    ACTIVE,
    PROBATION,
    ON_LEAVE,
    TERMINATED;

    public static EmployeeStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ACTIVE;
        }
        try {
            return EmployeeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ACTIVE;
        }
    }
}
