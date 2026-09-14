package com.hrm.employeemanagement.application.dto.conflict;

import java.math.BigDecimal;

public record ReplacementCandidateResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String departmentName,
        Long skillId,
        String skillName,
        Integer proficiencyLevel,
        String proficiencyLevelName,
        BigDecimal freeHours,
        Integer standardHoursPerWeek,
        BigDecimal totalAllocatedHours,
        String contractEndDate
) {
}
