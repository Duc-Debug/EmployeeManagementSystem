package com.hrm.employeemanagement.application.dto.conflict;

/**
 * Command to assign or unassign a handler for a schedule conflict warning.
 * Passing {@code assignedHandlerId = null} unassigns the currently assigned handler.
 */
public record AssignScheduleConflictHandlerCommand(
        Long conflictId,
        Long assignedHandlerId
) {
}
