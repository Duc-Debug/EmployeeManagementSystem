package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;

public record EmployeeSnapshotCellResult(
        int year,
        int weekNumber,
        BigDecimal allocatedHours,
        BigDecimal availableHours
) {
}
