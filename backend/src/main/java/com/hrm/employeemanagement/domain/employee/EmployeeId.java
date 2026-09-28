package com.hrm.employeemanagement.domain.employee;

public record EmployeeId(Long value) {
    public EmployeeId {
        if (value == null) {
            throw new IllegalArgumentException("EmployeeId value không được null");
        }
    }
}
