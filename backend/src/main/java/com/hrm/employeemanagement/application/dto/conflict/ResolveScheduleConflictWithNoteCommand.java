package com.hrm.employeemanagement.application.dto.conflict;

public record ResolveScheduleConflictWithNoteCommand(
        Long conflictId,
        Long assignedHandlerId,
        String resolutionNote
) {
}
