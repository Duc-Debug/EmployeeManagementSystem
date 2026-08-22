package com.hrm.employeemanagement.domain.department;

/**
 * Immutable Value Object representing the identity of a Department.
 */
public record DepartmentId(Long value) {
    public DepartmentId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("Department ID must be a positive integer");
        }
    }
}
