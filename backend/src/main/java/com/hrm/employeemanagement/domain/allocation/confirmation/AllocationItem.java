package com.hrm.employeemanagement.domain.allocation.confirmation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AllocationItem(
        Long allocationId,
        Long projectId,
        String projectName,
        String projectStatus,
        BigDecimal allocatedHours,
        LocalDateTime updatedAt
) {
}