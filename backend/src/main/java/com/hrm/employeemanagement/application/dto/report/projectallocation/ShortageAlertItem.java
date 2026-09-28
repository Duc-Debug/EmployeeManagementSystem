package com.hrm.employeemanagement.application.dto.report.projectallocation;

import java.math.BigDecimal;

public record ShortageAlertItem(
        int year,
        int weekNumber,
        String weekLabel,
        Long roleId,
        String roleName,
        BigDecimal demandHours,
        BigDecimal allocatedHours,
        BigDecimal missingHours,
        String severity,
        String message
) {
}