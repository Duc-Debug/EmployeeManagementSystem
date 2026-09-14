package com.hrm.employeemanagement.application.port.inbound.conflict;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;

public interface ResolveScheduleConflictUseCase {
    ScheduleConflictResult resolveScheduleConflict(Long conflictId);
}
