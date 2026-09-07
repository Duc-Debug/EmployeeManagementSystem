package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;

public record WeeklyCapacityResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        Integer year,
        Integer weekNumber,
        Integer standardHours,
        BigDecimal netAvailableHours,
        BigDecimal totalAllocatedHours,
        BigDecimal remainingAvailableHours
        ) {

}
