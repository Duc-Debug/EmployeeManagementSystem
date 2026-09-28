package com.hrm.employeemanagement.application.dto.conflict;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;

public record ScheduleConflictQuery(
        Integer yearNumber,
        Integer startWeek,
        Integer endWeek,
        Long employeeId,
        Long projectId,
        ConflictType conflictType,
        ScheduleConflictStatus status
) {
}
