package com.hrm.employeemanagement.application.dto.conflict;

public record AssignScheduleConflictHandlerCommand(
        Long conflictId,
        Long assignedHandlerId
) {
}
