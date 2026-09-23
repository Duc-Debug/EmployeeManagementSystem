package com.hrm.employeemanagement.application.dto.report.projectallocation;

import java.math.BigDecimal;

public record AllocatedMemberDetailItem(
        Long employeeId,
        String employeeCode,
        String fullName,
        String professionalRole,
        BigDecimal allocatedHours,
        BigDecimal allocationPercentage
) {
}