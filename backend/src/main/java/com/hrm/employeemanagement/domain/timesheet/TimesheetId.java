package com.hrm.employeemanagement.domain.timesheet;

import java.io.Serializable;
import java.util.Objects;

public record TimesheetId(Long value) implements Serializable {
    public TimesheetId {
        Objects.requireNonNull(value, "TimesheetId value must not be null");
    }
}
