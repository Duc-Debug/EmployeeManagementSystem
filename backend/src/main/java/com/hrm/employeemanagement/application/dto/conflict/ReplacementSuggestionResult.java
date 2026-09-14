package com.hrm.employeemanagement.application.dto.conflict;

import java.math.BigDecimal;
import java.util.List;

public record ReplacementSuggestionResult(
        Long conflictId,
        Long conflictedEmployeeId,
        String conflictedEmployeeCode,
        String conflictedEmployeeName,
        String departmentName,
        Integer yearNumber,
        Integer weekNumber,
        String weekLabel,
        Long skillId,
        String skillName,
        Integer requiredProficiencyLevel,
        BigDecimal excessHours,
        List<ReplacementCandidateResult> candidates,
        boolean hasAvailableReplacements,
        String recommendationMessage
) {
}
