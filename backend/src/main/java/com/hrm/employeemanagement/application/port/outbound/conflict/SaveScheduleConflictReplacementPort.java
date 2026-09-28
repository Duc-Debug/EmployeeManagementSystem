package com.hrm.employeemanagement.application.port.outbound.conflict;

import com.hrm.employeemanagement.domain.conflict.ScheduleConflictReplacement;

public interface SaveScheduleConflictReplacementPort {
    ScheduleConflictReplacement save(ScheduleConflictReplacement replacement);
}
