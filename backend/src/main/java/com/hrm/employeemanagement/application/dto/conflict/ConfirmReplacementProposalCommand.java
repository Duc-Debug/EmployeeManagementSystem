package com.hrm.employeemanagement.application.dto.conflict;

public record ConfirmReplacementProposalCommand(
        Long conflictId,
        Long replacementEmployeeId,
        Long skillId,
        Integer proficiencyLevel,
        String notes
) {
}
