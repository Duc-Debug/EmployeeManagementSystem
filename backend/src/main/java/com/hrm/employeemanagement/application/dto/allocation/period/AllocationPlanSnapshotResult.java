package com.hrm.employeemanagement.application.dto.allocation.period;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AllocationPlanSnapshotResult(
        Long id,
        Long periodId,
        int snapshotVersion,
        int totalAllocations,
        BigDecimal totalAllocatedHours,
        Long createdBy,
        LocalDateTime createdAt,
        List<AllocationPlanSnapshotItemResult> items
) {
}
