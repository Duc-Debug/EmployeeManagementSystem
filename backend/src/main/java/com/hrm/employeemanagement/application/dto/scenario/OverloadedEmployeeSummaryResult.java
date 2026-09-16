package com.hrm.employeemanagement.application.dto.scenario;

import java.math.BigDecimal;

public record OverloadedEmployeeSummaryResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        String professionalRole,
        int overloadedWeeksCount,
        BigDecimal maxExcessHours
) {
}
