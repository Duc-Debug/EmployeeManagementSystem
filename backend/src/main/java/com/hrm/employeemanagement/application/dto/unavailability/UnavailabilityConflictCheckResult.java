package com.hrm.employeemanagement.application.dto.unavailability;

import java.math.BigDecimal;
import java.util.List;

public record UnavailabilityConflictCheckResult(
        boolean hasConflict,
        int conflictingAllocationsCount,
        BigDecimal totalConflictingHours,
        List<ConflictingAllocationInfo> conflictingAllocations,
        String warningMessage
) {
    public record ConflictingAllocationInfo(
            Long allocationId,
            Long projectId,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours
    ) {}
}
