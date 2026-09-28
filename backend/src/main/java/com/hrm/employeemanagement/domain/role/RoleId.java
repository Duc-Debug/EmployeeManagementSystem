package com.hrm.employeemanagement.domain.role;

public record RoleId(Long value) {
    public RoleId {
        if (value == null) {
            throw new IllegalArgumentException("RoleId value không được null");
        }
    }
}
