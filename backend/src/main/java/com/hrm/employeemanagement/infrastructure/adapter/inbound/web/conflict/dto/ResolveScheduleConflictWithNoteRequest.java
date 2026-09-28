package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict.dto;

public record ResolveScheduleConflictWithNoteRequest(
        Long assignedHandlerId,
        String resolutionNote
) {}
