package com.hrm.employeemanagement.application.dto.report.projectallocation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyProjectSummaryItem(
        int year,
        int weekNumber,
        LocalDate startDate,
        LocalDate endDate,
        String weekLabel,
        BigDecimal demandHours,
        BigDecimal allocatedHours,
        BigDecimal shortfallHours,
        BigDecimal surplusHours,
        BigDecimal fulfillmentRate,
        String status
) {
}