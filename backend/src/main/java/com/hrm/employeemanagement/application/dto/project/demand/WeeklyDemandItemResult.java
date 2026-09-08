package com.hrm.employeemanagement.application.dto.project.demand;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyDemandItemResult(
        int year,
        int weekNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal requiredHours) {
}