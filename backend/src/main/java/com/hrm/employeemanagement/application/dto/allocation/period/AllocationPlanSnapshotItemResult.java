package com.hrm.employeemanagement.application.dto.allocation.period;

import java.math.BigDecimal;

public record AllocationPlanSnapshotItemResult(
        Long id,
        Long snapshotId,
        Long originalAllocationId,
        Long employeeId,
        Long projectId,
        int year,
        int weekNumber,
        BigDecimal allocatedHours,
        BigDecimal allocationPercentage,
        boolean isOverloaded,
        String overloadReason
) {
}
