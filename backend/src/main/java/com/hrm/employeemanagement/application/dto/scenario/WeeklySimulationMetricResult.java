package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;

public record WeeklySimulationMetricResult(
        int year,
        int weekNumber,
        String weekLabel,
        BigDecimal snapshotAllocatedHours,
        BigDecimal demandHours,
        BigDecimal scenarioWorkloadHours,
        BigDecimal availableHours,
        BigDecimal remainingHours,
        BigDecimal excessHours,
        BigDecimal utilizationPercentage,
        CapacityStatus status,
        boolean isOverloaded
) {
}
