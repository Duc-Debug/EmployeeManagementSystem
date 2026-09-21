package com.hrm.employeemanagement.application.dto.report.billablerate;

import java.math.BigDecimal;

public record BillableRateItem(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String orgUnitName,
        BigDecimal standardHours,
        BigDecimal holidayHours,
        BigDecimal approvedLeaveHours,
        BigDecimal netAvailableHours,
        BigDecimal billableHours,
        BigDecimal nonBillableHours,
        BigDecimal totalActualHours,
        BigDecimal billableRate,
        boolean hasAvailableHours,
        String status
) {
}
