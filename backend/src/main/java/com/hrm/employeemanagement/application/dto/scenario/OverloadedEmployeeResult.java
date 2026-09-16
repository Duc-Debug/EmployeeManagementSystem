package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.allocation.CapacityStatus;

public record OverloadedEmployeeResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        String professionalRole,
        int year,
        int weekNumber,
        String weekLabel,
        BigDecimal allocatedHours,
        BigDecimal availableHours,
        BigDecimal excessHours,
        BigDecimal utilizationPercentage,
        CapacityStatus status
) {
}

