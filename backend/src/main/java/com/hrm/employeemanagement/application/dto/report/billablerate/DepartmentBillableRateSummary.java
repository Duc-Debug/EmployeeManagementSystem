package com.hrm.employeemanagement.application.dto.report.billablerate;

import java.math.BigDecimal;

public record DepartmentBillableRateSummary(
        Long orgUnitId,
        String orgUnitName,
        BigDecimal totalStandardHours,
        BigDecimal totalApprovedLeaveHours,
        BigDecimal totalAvailableHours,
        BigDecimal totalBillableHours,
        BigDecimal totalNonBillableHours,
        BigDecimal totalActualHours,
        BigDecimal billableRate,
        int employeeCount
) {
}
