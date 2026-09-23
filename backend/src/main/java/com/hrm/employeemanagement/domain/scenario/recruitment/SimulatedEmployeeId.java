package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.util.Objects;

public record SimulatedEmployeeId(Long value) {
    public SimulatedEmployeeId {
        Objects.requireNonNull(value, "SimulatedEmployeeId value must not be null");
    }
}
