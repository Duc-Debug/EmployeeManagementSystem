package com.hrm.employeemanagement.application.dto.unavailability;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UnavailabilityPreviewResult(
        LocalDate startDate,
        LocalDate endDate,
        int workingDays,
        BigDecimal totalHoursDeducted
) {}
