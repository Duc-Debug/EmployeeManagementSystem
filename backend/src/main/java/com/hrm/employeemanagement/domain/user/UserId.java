package com.hrm.employeemanagement.domain.user;

public record UserId(Long value) {
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId value không được null");
        }
    }
}
