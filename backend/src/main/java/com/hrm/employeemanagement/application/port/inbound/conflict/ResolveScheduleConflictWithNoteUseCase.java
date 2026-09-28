package com.hrm.employeemanagement.application.port.inbound.conflict;

import com.hrm.employeemanagement.application.dto.conflict.ResolveScheduleConflictWithNoteCommand;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;

public interface ResolveScheduleConflictWithNoteUseCase {
    ScheduleConflictResult resolveScheduleConflictWithNote(ResolveScheduleConflictWithNoteCommand command);
}
