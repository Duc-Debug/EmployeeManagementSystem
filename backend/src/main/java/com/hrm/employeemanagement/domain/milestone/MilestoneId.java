package com.hrm.employeemanagement.domain.milestone;

import java.util.Objects;

public record MilestoneId(Long value) {
    public MilestoneId {
        Objects.requireNonNull(value, "MilestoneId value must not be null");
    }
}
