package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;

public record ProjectWeeklyAllocationResult(
        Long employeeId,
        Long projectId,
        Integer year,
        Integer weekNumber,
        BigDecimal allocatedHours) {
}
