package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;

public record AdjustAllocationCommand(
        AdjustmentAction action,
        BigDecimal newHours,
        BigDecimal allocationPercentage,
        Integer targetYear,
        Integer targetWeek,
        String varianceReason,
        String overloadReason
) {
}
