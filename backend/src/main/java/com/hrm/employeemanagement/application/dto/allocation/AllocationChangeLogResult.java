package com.hrm.employeemanagement.application.dto.allocation;

import java.time.LocalDateTime;
import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;

public record AllocationChangeLogResult(
        Long id,
        Long allocationId,
        AdjustmentAction action,
        String oldValue,
        String newValue,
        Long changedBy,
        String changedByName,
        LocalDateTime changedAt,
        String notifiedPmIds
) {
}
