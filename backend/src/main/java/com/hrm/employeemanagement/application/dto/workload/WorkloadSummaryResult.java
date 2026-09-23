package com.hrm.employeemanagement.application.dto.workload;

import java.math.BigDecimal;

public record WorkloadSummaryResult(
        BigDecimal totalStandardHours,
        BigDecimal totalHolidayHours,
        BigDecimal totalApprovedLeaveHours,
        BigDecimal totalNetAvailableHours,
        BigDecimal totalAllocatedHours,
        BigDecimal averageUtilizationPercentage,
        int overloadedWeeksCount,
        int normalWeeksCount,
        int idleWeeksCount,
        BigDecimal maxUtilizationPercentage,
        String maxUtilizationWeek
) {}
