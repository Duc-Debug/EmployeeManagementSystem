package com.hrm.employeemanagement.application.dto.conflict;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReplacementProposalResult(
        Long proposalId,
        Long conflictId,
        Long originalEmployeeId,
        String originalEmployeeName,
        Long replacementEmployeeId,
        String replacementEmployeeName,
        Long skillId,
        String skillName,
        Integer proficiencyLevel,
        BigDecimal freeHours,
        String status,
        String notes,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt
) {
}
