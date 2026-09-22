package com.hrm.employeemanagement.application.dto.workload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyWorkloadItemResult(
        int year,
        int weekNumber,
        LocalDate startDate,
        LocalDate endDate,
        String weekLabel,
        int standardHours,
        int holidayHours,
        BigDecimal approvedLeaveHours,
        BigDecimal netAvailableHours,
        BigDecimal totalAllocatedHours,
        BigDecimal utilizationPercentage,
        String status,
        BigDecimal overloadHours,
        List<ProjectWorkloadAllocationResult> projectAllocations
) {}
