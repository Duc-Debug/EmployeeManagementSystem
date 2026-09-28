package com.hrm.employeemanagement.domain.project;

public record ProjectId(Long value) {
    public ProjectId {
        if (value == null) {
            throw new IllegalArgumentException("ProjectId value khong duoc null");
        }
    }
}
