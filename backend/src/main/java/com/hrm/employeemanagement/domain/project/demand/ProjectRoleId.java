package com.hrm.employeemanagement.domain.project.demand;

public record ProjectRoleId(Long value) {
    public ProjectRoleId {
        if (value == null) {
            throw new IllegalArgumentException("ProjectRoleId value không được null");
        }
    }
}
