package com.hrm.employeemanagement.application.dto.report.billablerate;

import java.math.BigDecimal;

public record BillableRateSummary(
        BigDecimal totalStandardHours,
        BigDecimal totalHolidayHours,
        BigDecimal totalApprovedLeaveHours,
        BigDecimal totalAvailableHours,
        BigDecimal totalBillableHours,
        BigDecimal totalNonBillableHours,
        BigDecimal totalActualHours,
        BigDecimal overallBillableRate,
        int totalEmployees,
        int totalDepartments,
        int targetWeeksCount
) {
}
