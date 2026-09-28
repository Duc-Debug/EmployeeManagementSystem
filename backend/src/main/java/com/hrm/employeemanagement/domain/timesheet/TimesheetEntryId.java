package com.hrm.employeemanagement.domain.timesheet;

import java.io.Serializable;
import java.util.Objects;

public record TimesheetEntryId(Long value) implements Serializable {
    public TimesheetEntryId {
        Objects.requireNonNull(value, "TimesheetEntryId value must not be null");
    }
}
