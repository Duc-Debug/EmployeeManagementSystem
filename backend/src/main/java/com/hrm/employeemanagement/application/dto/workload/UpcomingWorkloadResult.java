package com.hrm.employeemanagement.application.dto.workload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UpcomingWorkloadResult(
        Long employeeId,
        String employeeCode,
        String employeeName,
        Long orgUnitId,
        String orgUnitName,
        int fromYear,
        int fromWeek,
        int durationWeeks,
        BigDecimal effectiveOverloadThreshold,
        BigDecimal effectiveIdleThreshold,
        List<WeeklyWorkloadItemResult> weeklyWorkloads,
        WorkloadSummaryResult summary,
        LocalDateTime generatedAt
) {}
