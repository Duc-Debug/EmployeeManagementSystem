package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;

public record EmployeeSnapshotCellResult(
        int year,
        int weekNumber,
        BigDecimal allocatedHours,
        BigDecimal availableHours
        BigDecimal availableHours,
        BigDecimal excessHours,
        BigDecimal utilizationPercentage,
        CapacityStatus status,
        boolean isOverloaded
) {
    public EmployeeSnapshotCellResult(int year, int weekNumber, BigDecimal allocatedHours, BigDecimal availableHours) {
        this(year, weekNumber, allocatedHours, availableHours, BigDecimal.ZERO, null, CapacityStatus.OPTIMAL, false);
    }
}
