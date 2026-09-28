package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;

public record ProjectWeeklyAllocationResult(
        Long id,
        Long employeeId,
        Long projectId,
        Long projectRoleId,
        Integer year,
        Integer weekNumber,
        BigDecimal allocatedHours,
        BigDecimal allocationPercentage,
        String varianceNote) {

    public ProjectWeeklyAllocationResult(
            Long id,
            Long employeeId,
            Long projectId,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours,
            BigDecimal allocationPercentage,
            String varianceNote) {
        this(id, employeeId, projectId, null, year, weekNumber, allocatedHours, allocationPercentage, varianceNote);
    }

    public ProjectWeeklyAllocationResult(
            Long employeeId,
            Long projectId,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours) {
        this(null, employeeId, projectId, null, year, weekNumber, allocatedHours, null, null);
    }
}
