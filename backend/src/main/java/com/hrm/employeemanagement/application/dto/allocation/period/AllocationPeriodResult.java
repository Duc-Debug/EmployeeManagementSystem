package com.hrm.employeemanagement.application.dto.allocation.period;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodType;

public record AllocationPeriodResult(
        Long id,
        String name,
        AllocationPeriodType periodType,
        int year,
        int startWeek,
        int endWeek,
        AllocationPeriodStatus status,
        Long lockedBy,
        LocalDateTime lockedAt,
        Long unlockedBy,
        LocalDateTime unlockedAt,
        String unlockReason,
        Long createdBy,
        LocalDateTime createdAt,
        AllocationPlanSnapshotResult latestSnapshot
) {
}
